import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Badge, Button, Card, PageHeader, Spinner, statusTone } from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDate, rupees, titleCase } from '../lib/format'

/**
 * Plans, checkout and the receipt history.
 *
 * With no Razorpay key configured the backend reports {@code stubMode}, and checkout completes
 * without the gateway — which is what lets the whole flow be demonstrated offline.
 */
export default function Premium() {
  const { refreshUser } = useAuth()
  const queryClient = useQueryClient()
  const [error, setError] = useState('')

  const plans = useQuery({ queryKey: ['plans'], queryFn: api.billing.plans })
  const subscription = useQuery({
    queryKey: ['subscription'],
    queryFn: api.billing.currentSubscription,
  })
  const orders = useQuery({ queryKey: ['orders'], queryFn: () => api.billing.orders({ size: 10 }) })

  const checkout = useMutation({
    mutationFn: async (planId) => {
      const order = await api.billing.createOrder(planId)
      if (order.stubMode) {
        // No gateway configured: confirm straight away with a stub signature.
        return api.billing.verify({
          razorpayOrderId: order.gatewayOrderId,
          razorpayPaymentId: `pay_stub_${Date.now()}`,
          razorpaySignature: 'stub-signature',
        })
      }
      return openRazorpay(order)
    },
    onSuccess: async () => {
      await refreshUser()
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  const cancel = useMutation({
    mutationFn: () => api.billing.cancel('No longer needed'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['subscription'] }),
  })

  if (plans.isLoading) return <Spinner />

  return (
    <>
      <PageHeader title="Premium" subtitle="Unlimited AI analyses and priority on referrals" />

      {subscription.data?.current && (
        <Card className="mb-6 bg-brand-50 border-brand-100">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-medium">{subscription.data.planName} is active</p>
              <p className="text-sm text-slate-600">
                {subscription.data.daysRemaining} days left · renews {formatDate(subscription.data.expiresAt)}
              </p>
            </div>
            {subscription.data.status === 'ACTIVE' && (
              <Button variant="secondary" onClick={() => cancel.mutate()} loading={cancel.isPending}>
                Cancel renewal
              </Button>
            )}
          </div>
        </Card>
      )}

      {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

      <div className="grid md:grid-cols-2 gap-5">
        {plans.data?.map((plan) => (
          <Card key={plan.id} className={plan.recommended ? 'ring-2 ring-brand-500' : undefined}>
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-lg">{plan.name}</h2>
                <p className="text-sm text-slate-500">{plan.description}</p>
              </div>
              {plan.recommended && <Badge tone="brand">Best value</Badge>}
            </div>

            <p className="text-3xl font-semibold mt-4">
              {plan.priceLabel}
              <span className="text-sm font-normal text-slate-500"> / {plan.durationDays} days</span>
            </p>

            <ul className="text-sm text-slate-700 mt-4 space-y-1.5">
              {plan.features?.map((feature) => (
                <li key={feature}>· {feature}</li>
              ))}
            </ul>

            <Button
              className="mt-5 w-full"
              onClick={() => checkout.mutate(plan.id)}
              loading={checkout.isPending}
            >
              Choose {plan.name}
            </Button>
          </Card>
        ))}
      </div>

      <Card className="mt-6">
        <h2 className="text-base mb-3">Payment history</h2>
        {orders.data?.content?.length ? (
          <table className="w-full text-sm">
            <thead className="text-left text-slate-500">
              <tr>
                <th className="py-2">Plan</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {orders.data.content.map((order) => (
                <tr key={order.id} className="border-t border-slate-100">
                  <td className="py-2">{order.planName}</td>
                  <td>{rupees(order.amountPaise)}</td>
                  <td>
                    <Badge tone={statusTone(order.status)}>{titleCase(order.status)}</Badge>
                  </td>
                  <td className="text-slate-500">{formatDate(order.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="text-sm text-slate-500">No payments yet.</p>
        )}
      </Card>
    </>
  )
}

/**
 * Loads Razorpay's widget on demand and resolves once the payment is verified by our backend.
 * Nothing is imported at build time, so a project without a gateway carries no dead dependency.
 */
function openRazorpay(order) {
  return new Promise((resolve, reject) => {
    const start = () => {
      const checkout = new window.Razorpay({
        key: order.keyId,
        order_id: order.gatewayOrderId,
        amount: order.amountPaise,
        currency: order.currency,
        name: 'LegacyLoop',
        description: order.planName,
        prefill: { email: order.userEmail },
        handler: (response) =>
          api.billing
            .verify({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })
            .then(resolve)
            .catch(reject),
        modal: { ondismiss: () => reject(new Error('Payment was cancelled')) },
      })
      checkout.open()
    }

    if (window.Razorpay) return start()

    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.onload = start
    script.onerror = () => reject(new Error('Could not load the payment window'))
    document.body.appendChild(script)
  })
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  PageHeader,
  Spinner,
  Tabs,
  statusTone,
} from '../components/ui'
import { api } from '../lib/api'
import { formatDate, titleCase } from '../lib/format'

/** Both sides of a referral on one screen: what you were asked, and what you asked for. */
export default function Referrals() {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState('received')

  const referrals = useQuery({
    queryKey: ['referrals', tab],
    queryFn: () =>
      tab === 'received'
        ? api.applications.referralsReceived({ size: 30 })
        : api.applications.referralsSent({ size: 30 }),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['referrals'] })

  const respond = useMutation({
    mutationFn: ({ id, decision }) => api.applications.respondToReferral(id, { decision }),
    onSuccess: invalidate,
  })

  const withdraw = useMutation({ mutationFn: api.applications.withdrawReferral, onSuccess: invalidate })

  return (
    <>
      <PageHeader title="Referrals" subtitle="Requests to put someone forward" />

      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { value: 'received', label: 'Asked of you' },
          { value: 'sent', label: 'You asked' },
        ]}
      />

      {referrals.isLoading && <Spinner />}
      {referrals.data?.content?.length === 0 && (
        <EmptyState
          title="Nothing here"
          message={
            tab === 'received'
              ? 'When a student asks you for a referral it will show up here.'
              : 'Ask an alumnus from one of your applications.'
          }
        />
      )}

      <div className="space-y-3">
        {referrals.data?.content?.map((referral) => (
          <Card key={referral.id}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium">{referral.jobTitle}</p>
                <p className="text-sm text-slate-500">
                  {referral.companyName} ·{' '}
                  {tab === 'received'
                    ? `from ${referral.requesterName}`
                    : `to ${referral.referrerName ?? 'an alumnus'}`}{' '}
                  · {formatDate(referral.requestedAt)}
                </p>
              </div>
              <Badge tone={statusTone(referral.status)}>{titleCase(referral.status)}</Badge>
            </div>

            {referral.message && (
              <p className="text-sm text-slate-700 bg-slate-50 rounded-lg p-3 mt-3">{referral.message}</p>
            )}

            {referral.status === 'REQUESTED' && (
              <div className="flex gap-2 mt-4">
                {tab === 'received' ? (
                  <>
                    <Button onClick={() => respond.mutate({ id: referral.id, decision: 'ACCEPT' })}>
                      Refer them
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={() => respond.mutate({ id: referral.id, decision: 'DECLINE' })}
                    >
                      Not this time
                    </Button>
                  </>
                ) : (
                  <Button variant="ghost" onClick={() => withdraw.mutate(referral.id)}>
                    Withdraw request
                  </Button>
                )}
              </div>
            )}
          </Card>
        ))}
      </div>
    </>
  )
}

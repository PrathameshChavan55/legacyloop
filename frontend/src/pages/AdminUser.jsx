import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Badge, Button, Card, PageHeader, Spinner, statusTone } from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { formatDateTime, roleLabel, titleCase } from '../lib/format'

/** One account: its details, its sign-ins, and the actions an admin can take on it. */
export default function AdminUser() {
  const { userId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const user = useQuery({ queryKey: ['admin', 'user', userId], queryFn: () => api.admin.user(userId) })
  const logins = useQuery({
    queryKey: ['admin', 'user', userId, 'logins'],
    queryFn: () => api.admin.loginHistory(userId, { size: 10 }),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin'] })
  const act = useMutation({
    mutationFn: ({ action, body }) => api.admin[action](userId, body),
    onSuccess: () => {
      invalidate()
    },
  })
  const deleteMutation = useMutation({
    mutationFn: () => api.admin.deleteUser(userId),
    onSuccess: () => {
      invalidate()
      navigate('/admin')
    },
  })

  if (user.isLoading) return <Spinner />
  const account = user.data

  return (
    <div className="max-w-3xl">
      <Link to="/admin" className="text-sm link">
        ← Back to administration
      </Link>

      <PageHeader
        title={account.fullName}
        subtitle={account.email}
        action={<Badge tone={statusTone(account.status)}>{account.statusLabel}</Badge>}
      />

      <Card>
        <div className="grid sm:grid-cols-2 gap-4 text-sm">
          <Detail label="Roles" value={account.roles?.map(roleLabel).join(', ')} />
          <Detail label="Identifier" value={account.studentIdentifier ?? '—'} />
          <Detail label="Email verified" value={account.emailVerified ? 'Yes' : 'No'} />
          <Detail label="Premium" value={account.premium ? 'Yes' : 'No'} />
          <Detail label="Joined" value={formatDateTime(account.createdAt)} />
          <Detail label="Last sign-in" value={formatDateTime(account.lastLoginAt) || 'Never'} />
        </div>

        <div className="flex flex-wrap gap-2 mt-5">
          {(account.status === 'PENDING_APPROVAL' || account.status === 'PENDING_VERIFICATION') && (
            <Button
              loading={act.isPending}
              onClick={() => act.mutate({ action: 'verify' })}
            >
              Verify & Activate
            </Button>
          )}
          {account.status === 'ACTIVE' && (
            <Button
              variant="secondary"
              loading={act.isPending}
              onClick={() => act.mutate({ action: 'suspend', body: 'Suspended by admin' })}
            >
              Suspend
            </Button>
          )}
          {account.status === 'SUSPENDED' && (
            <Button
              loading={act.isPending}
              onClick={() => act.mutate({ action: 'reactivate' })}
            >
              Reactivate
            </Button>
          )}
          <Button
            variant="ghost"
            loading={act.isPending}
            onClick={() => act.mutate({ action: 'forcePasswordReset' })}
          >
            Email a temporary password
          </Button>
          <Button
            variant="secondary"
            className="text-red-600 hover:bg-red-50 hover:border-red-200 ml-auto"
            loading={deleteMutation.isPending}
            onClick={() => {
              if (window.confirm(`Are you sure you want to completely delete ${account.fullName} (${account.email})?`)) {
                deleteMutation.mutate()
              }
            }}
          >
            Delete profile & account
          </Button>
        </div>
      </Card>

      <Card className="mt-4">
        <h2 className="text-base mb-3">Recent sign-ins</h2>
        {logins.data?.content?.length ? (
          <ul className="text-sm divide-y divide-slate-100">
            {logins.data.content.map((entry) => (
              <li key={entry.id} className="py-2 flex justify-between">
                <span className={entry.action === 'LOGIN_FAILED' ? 'text-red-600' : 'text-slate-700'}>
                  {titleCase(entry.action)}
                  {entry.detail && ` · ${entry.detail}`}
                </span>
                <span className="text-slate-400">{formatDateTime(entry.createdAt)}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-slate-500">No sign-ins recorded.</p>
        )}
      </Card>
    </div>
  )
}

const Detail = ({ label, value }) => (
  <div>
    <p className="text-xs uppercase tracking-wide text-slate-400">{label}</p>
    <p className="font-medium text-slate-800">{value}</p>
  </div>
)

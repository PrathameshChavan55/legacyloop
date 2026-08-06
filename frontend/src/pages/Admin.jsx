import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  Tabs,
  statusTone,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { formatDateTime, roleLabel, rupees, titleCase } from '../lib/format'

/** The admin console: accounts, institutions, plans and the audit trail on one screen. */
export default function Admin() {
  const [tab, setTab] = useState('users')
  const statistics = useQuery({ queryKey: ['admin', 'statistics'], queryFn: api.admin.statistics })

  return (
    <>
      <PageHeader title="Administration" subtitle="Accounts, institutions, plans and the audit trail" />

      {statistics.data && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <Stat label="Accounts" value={statistics.data.total} />
          <Stat label="Awaiting approval" value={statistics.data.pendingApproval} />
          <Stat label="Suspended" value={statistics.data.suspended} />
          <Stat label="Premium" value={statistics.data.premium} />
        </div>
      )}

      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { value: 'users', label: 'Accounts' },
          { value: 'institutions', label: 'Institutions' },
          { value: 'plans', label: 'Plans' },
          { value: 'audit', label: 'Audit trail' },
        ]}
      />

      {tab === 'users' && <Users />}
      {tab === 'institutions' && <Institutions />}
      {tab === 'plans' && <Plans />}
      {tab === 'audit' && <Audit />}
    </>
  )
}

const Stat = ({ label, value }) => (
  <Card>
    <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
    <p className="text-3xl font-semibold mt-1">{value}</p>
  </Card>
)

function Users() {
  const queryClient = useQueryClient()
  const [filters, setFilters] = useState({ query: '', status: '' })
  const [page, setPage] = useState(0)
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({
    email: '',
    firstName: '',
    lastName: '',
    role: 'ROLE_INSTITUTION_STAFF',
    institutionId: '',
  })
  const [error, setError] = useState('')

  const users = useQuery({
    queryKey: ['admin', 'users', filters, page],
    queryFn: () => api.admin.users({ ...filters, page }),
  })
  const institutions = useQuery({ queryKey: ['branding'], queryFn: api.institutions.branding })

  const [actionError, setActionError] = useState('')

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin'] })
  const act = useMutation({
    mutationFn: ({ id, action, body }) => api.admin[action](id, body),
    onSuccess: () => {
      setActionError('')
      invalidate()
    },
    onError: (failure) => setActionError(errorMessage(failure)),
  })
  const create = useMutation({
    mutationFn: () => api.admin.create({ ...form, institutionId: Number(form.institutionId) }),
    onSuccess: () => {
      setAdding(false)
      invalidate()
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <>
      <Card className="mb-4 grid sm:grid-cols-3 gap-3">
        <Input
          placeholder="Name, email or identifier"
          value={filters.query}
          onChange={(event) => {
            setFilters({ ...filters, query: event.target.value })
            setPage(0)
          }}
        />
        <Select
          value={filters.status}
          onChange={(event) => {
            setFilters({ ...filters, status: event.target.value })
            setPage(0)
          }}
        >
          <option value="">Any status</option>
          {['PENDING_VERIFICATION', 'PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED'].map((status) => (
            <option key={status} value={status}>
              {titleCase(status)}
            </option>
          ))}
        </Select>
        <Button onClick={() => setAdding(true)}>Create staff account</Button>
      </Card>

      {actionError && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg border border-red-200">
          {actionError}
        </div>
      )}

      {users.isLoading && <Spinner />}

      <div className="space-y-2">
        {users.data?.content?.map((user) => (
          <Card key={user.id} className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <Link to={`/admin/users/${user.id}`} className="font-medium hover:underline">
                {user.fullName}
              </Link>
              <p className="text-sm text-slate-500">
                {user.email} · {user.roles?.map(roleLabel).join(', ')}
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Badge tone={statusTone(user.status)}>{user.statusLabel}</Badge>
              {(user.status === 'PENDING_APPROVAL' || user.status === 'PENDING_VERIFICATION') && (
                <Button
                  loading={act.isPending && act.variables?.id === user.id}
                  onClick={() => act.mutate({ id: user.id, action: 'verify' })}
                >
                  Verify & Activate
                </Button>
              )}
              {user.status === 'ACTIVE' && (
                <Button
                  variant="ghost"
                  loading={act.isPending && act.variables?.id === user.id}
                  onClick={() => act.mutate({ id: user.id, action: 'suspend', body: 'Suspended by admin' })}
                >
                  Suspend
                </Button>
              )}
              {user.status === 'SUSPENDED' && (
                <Button
                  variant="ghost"
                  loading={act.isPending && act.variables?.id === user.id}
                  onClick={() => act.mutate({ id: user.id, action: 'reactivate' })}
                >
                  Reactivate
                </Button>
              )}
              <Button
                variant="ghost"
                className="text-red-600 hover:text-red-700 hover:bg-red-50"
                loading={act.isPending && act.variables?.id === user.id && act.variables?.action === 'deleteUser'}
                onClick={() => {
                  if (window.confirm(`Are you sure you want to delete profile & account for ${user.fullName} (${user.email})?`)) {
                    act.mutate({ id: user.id, action: 'deleteUser' })
                  }
                }}
              >
                Delete
              </Button>
            </div>
          </Card>
        ))}
      </div>

      <Pagination page={page} totalPages={users.data?.totalPages} onChange={setPage} />

      <Modal
        open={adding}
        title="Create a staff or admin account"
        onClose={() => setAdding(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setAdding(false)}>
              Cancel
            </Button>
            <Button onClick={() => create.mutate()} loading={create.isPending}>
              Create
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-500 mb-4">
          A temporary password is emailed to them, and they must change it on first sign-in.
        </p>

        <div className="grid grid-cols-2 gap-3">
          <Field label="First name">
            <Input value={form.firstName} onChange={update('firstName')} />
          </Field>
          <Field label="Last name">
            <Input value={form.lastName} onChange={update('lastName')} />
          </Field>
        </div>

        <Field label="Email">
          <Input type="email" value={form.email} onChange={update('email')} />
        </Field>

        <Field label="Role">
          <Select value={form.role} onChange={update('role')}>
            <option value="ROLE_INSTITUTION_STAFF">Placement staff</option>
            <option value="ROLE_PLATFORM_ADMIN">Administrator</option>
          </Select>
        </Field>

        <Field label="Institution">
          <Select value={form.institutionId} onChange={update('institutionId')}>
            <option value="">Choose one</option>
            {institutions.data?.map((institution) => (
              <option key={institution.id} value={institution.id}>
                {institution.name}
              </option>
            ))}
          </Select>
        </Field>

        {error && <p className="text-sm text-red-600">{error}</p>}
      </Modal>
    </>
  )
}

function Institutions() {
  const queryClient = useQueryClient()
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({
    code: '',
    name: '',
    shortName: '',
    primaryColor: '#4f46e5',
    identifierLabel: 'Roll number',
    identifierPattern: '',
    staffRoleLabel: 'Placement staff',
    city: '',
  })
  const [error, setError] = useState('')

  const institutions = useQuery({
    queryKey: ['institutions'],
    queryFn: () => api.institutions.search({ size: 50 }),
  })

  const create = useMutation({
    mutationFn: () => api.institutions.create(form),
    onSuccess: () => {
      setAdding(false)
      queryClient.invalidateQueries({ queryKey: ['institutions'] })
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <>
      <div className="flex justify-end mb-3">
        <Button onClick={() => setAdding(true)}>Onboard an institution</Button>
      </div>

      {institutions.isLoading && <Spinner />}

      <div className="space-y-2">
        {institutions.data?.content?.map((institution) => (
          <Card key={institution.id} className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <span
                className="w-8 h-8 rounded-lg"
                style={{ backgroundColor: institution.primaryColor }}
                aria-hidden
              />
              <div>
                <p className="font-medium">{institution.name}</p>
                <p className="text-sm text-slate-500">
                  {institution.code} · calls it "{institution.identifierLabel}"
                </p>
              </div>
            </div>
            <Badge tone={institution.active ? 'green' : 'red'}>
              {institution.active ? 'Active' : 'Inactive'}
            </Badge>
          </Card>
        ))}
      </div>

      <Modal
        open={adding}
        title="Onboard an institution"
        onClose={() => setAdding(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setAdding(false)}>
              Cancel
            </Button>
            <Button onClick={() => create.mutate()} loading={create.isPending}>
              Create
            </Button>
          </>
        }
      >
        <div className="grid grid-cols-2 gap-3">
          <Field label="Code" hint="Upper case, e.g. DEMO">
            <Input value={form.code} onChange={update('code')} />
          </Field>
          <Field label="Short name">
            <Input value={form.shortName} onChange={update('shortName')} />
          </Field>
        </div>

        <Field label="Name">
          <Input value={form.name} onChange={update('name')} />
        </Field>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Brand colour">
            <Input type="color" value={form.primaryColor} onChange={update('primaryColor')} />
          </Field>
          <Field label="City">
            <Input value={form.city} onChange={update('city')} />
          </Field>
        </div>

        <Field label="What they call a student number">
          <Input value={form.identifierLabel} onChange={update('identifierLabel')} />
        </Field>

        <Field label="Identifier pattern" hint="A regex, or leave blank to accept anything">
          <Input value={form.identifierPattern} onChange={update('identifierPattern')} />
        </Field>

        <Field label="What they call placement staff">
          <Input value={form.staffRoleLabel} onChange={update('staffRoleLabel')} />
        </Field>

        {error && <p className="text-sm text-red-600">{error}</p>}
      </Modal>
    </>
  )
}

function Plans() {
  const queryClient = useQueryClient()
  const plans = useQuery({ queryKey: ['plans'], queryFn: api.billing.plans })
  const withdraw = useMutation({
    mutationFn: api.billing.withdrawPlan,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['plans'] }),
  })

  if (plans.isLoading) return <Spinner />
  if (plans.data?.length === 0) return <EmptyState title="No plans on sale" />

  return (
    <div className="space-y-2">
      {plans.data?.map((plan) => (
        <Card key={plan.id} className="flex items-center justify-between gap-3">
          <div>
            <p className="font-medium">
              {plan.name} <span className="text-slate-400 text-sm">({plan.code})</span>
            </p>
            <p className="text-sm text-slate-500">
              {rupees(plan.amountPaise)} for {plan.durationDays} days
            </p>
          </div>
          <div className="flex items-center gap-2">
            {plan.recommended && <Badge tone="brand">Recommended</Badge>}
            <Button variant="ghost" onClick={() => withdraw.mutate(plan.id)}>
              Withdraw
            </Button>
          </div>
        </Card>
      ))}
    </div>
  )
}

function Audit() {
  const [page, setPage] = useState(0)
  const logs = useQuery({
    queryKey: ['admin', 'audit', page],
    queryFn: () => api.admin.auditLogs({ page, size: 25 }),
  })

  if (logs.isLoading) return <Spinner />

  return (
    <>
      <Card className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="text-left text-slate-500">
            <tr>
              <th className="py-2">When</th>
              <th>Action</th>
              <th>Account</th>
              <th>Detail</th>
            </tr>
          </thead>
          <tbody>
            {logs.data?.content?.map((entry) => (
              <tr key={entry.id} className="border-t border-slate-100">
                <td className="py-2 text-slate-500 whitespace-nowrap">{formatDateTime(entry.createdAt)}</td>
                <td>{titleCase(entry.action)}</td>
                <td className="text-slate-600">{entry.email ?? entry.userId}</td>
                <td className="text-slate-500">{entry.detail}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Pagination page={page} totalPages={logs.data?.totalPages} onChange={setPage} />
    </>
  )
}

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
  Spinner,
  Tabs,
  Textarea,
  statusTone,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { titleCase } from '../lib/format'

/** Postings and employers on one screen, because they are managed in the same sitting. */
export default function ManageJobs() {
  const [tab, setTab] = useState('jobs')

  return (
    <>
      <PageHeader
        title="Manage"
        subtitle="Your postings and the employers behind them"
        action={
          <Link to="/jobs/new">
            <Button>New posting</Button>
          </Link>
        }
      />

      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { value: 'jobs', label: 'Postings' },
          { value: 'companies', label: 'Companies' },
        ]}
      />

      {tab === 'jobs' ? <JobList /> : <CompanyList />}
    </>
  )
}

function JobList() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)

  const jobs = useQuery({ queryKey: ['jobs', 'manage', page], queryFn: () => api.jobs.manage({ page }) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['jobs'] })

  const act = useMutation({
    mutationFn: ({ id, action }) => api.jobs[action](id),
    onSuccess: invalidate,
  })

  if (jobs.isLoading) return <Spinner />
  if (jobs.data?.content?.length === 0) {
    return <EmptyState title="No postings yet" message="Create one to start collecting applications." />
  }

  return (
    <>
      <div className="space-y-3">
        {jobs.data?.content?.map((job) => (
          <Card key={job.id} className="flex flex-wrap items-center gap-3 justify-between">
            <div>
              <Link to={`/jobs/${job.id}`} className="font-medium hover:underline">
                {job.title}
              </Link>
              <p className="text-sm text-slate-500">
                {job.company?.name} · {job.applicationCount} applicants
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Badge tone={statusTone(job.status)}>{titleCase(job.status)}</Badge>
              <Link to={`/jobs/${job.id}/applicants`}>
                <Button variant="secondary">Applicants</Button>
              </Link>
              <Link to={`/jobs/${job.id}/edit`}>
                <Button variant="ghost">Edit</Button>
              </Link>
              {job.status === 'DRAFT' && (
                <Button onClick={() => act.mutate({ id: job.id, action: 'publish' })}>Publish</Button>
              )}
              {job.status === 'OPEN' && (
                <Button variant="ghost" onClick={() => act.mutate({ id: job.id, action: 'close' })}>
                  Close
                </Button>
              )}
              {job.status === 'CLOSED' && (
                <Button variant="ghost" onClick={() => act.mutate({ id: job.id, action: 'reopen' })}>
                  Reopen
                </Button>
              )}
            </div>
          </Card>
        ))}
      </div>

      <Pagination page={page} totalPages={jobs.data?.totalPages} onChange={setPage} />
    </>
  )
}

function CompanyList() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({ name: '', industry: '', website: '', headquarters: '', description: '' })
  const [error, setError] = useState('')

  const companies = useQuery({
    queryKey: ['companies', page],
    queryFn: () => api.jobs.companies({ page }),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['companies'] })

  const create = useMutation({
    mutationFn: () => api.jobs.createCompany(form),
    onSuccess: () => {
      setAdding(false)
      setForm({ name: '', industry: '', website: '', headquarters: '', description: '' })
      invalidate()
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  const verify = useMutation({ mutationFn: api.jobs.verifyCompany, onSuccess: invalidate })

  if (companies.isLoading) return <Spinner />

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <>
      <div className="flex justify-end mb-3">
        <Button variant="secondary" onClick={() => setAdding(true)}>
          Add a company
        </Button>
      </div>

      <div className="space-y-3">
        {companies.data?.content?.map((company) => (
          <Card key={company.id} className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-medium">{company.name}</p>
              <p className="text-sm text-slate-500">
                {[company.industry, company.headquarters].filter(Boolean).join(' · ') || 'No details yet'}
              </p>
            </div>
            <div className="flex items-center gap-2">
              {company.verified ? (
                <Badge tone="green">Verified</Badge>
              ) : (
                <Button variant="secondary" onClick={() => verify.mutate(company.id)}>
                  Mark verified
                </Button>
              )}
            </div>
          </Card>
        ))}
      </div>

      <Pagination page={page} totalPages={companies.data?.totalPages} onChange={setPage} />

      <Modal
        open={adding}
        title="Add a company"
        onClose={() => setAdding(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setAdding(false)}>
              Cancel
            </Button>
            <Button onClick={() => create.mutate()} loading={create.isPending}>
              Add
            </Button>
          </>
        }
      >
        <Field label="Name">
          <Input required value={form.name} onChange={update('name')} />
        </Field>
        <Field label="Industry">
          <Input value={form.industry} onChange={update('industry')} />
        </Field>
        <Field label="Website">
          <Input value={form.website} onChange={update('website')} />
        </Field>
        <Field label="Headquarters">
          <Input value={form.headquarters} onChange={update('headquarters')} />
        </Field>
        <Field label="About">
          <Textarea value={form.description} onChange={update('description')} />
        </Field>
        {error && <p className="text-sm text-red-600">{error}</p>}
      </Modal>
    </>
  )
}

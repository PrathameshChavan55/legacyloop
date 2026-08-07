import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Field, Input, PageHeader, Select, Spinner, Textarea } from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { JOB_TYPES, WORK_MODES, titleCase } from '../lib/format'

const EMPTY = {
  title: '',
  description: '',
  responsibilities: '',
  requirements: '',
  companyId: '',
  jobType: 'FULL_TIME',
  workMode: 'ONSITE',
  location: '',
  salaryMin: '',
  salaryMax: '',
  minCgpa: '',
  maxBacklogs: '',
  applicationDeadline: '',
  vacancies: '',
  requiredSkills: '',
  eligibleBatchIds: [],
  referralsEnabled: true,
}

/** One form for both creating and editing — the route decides which. */
export default function JobForm() {
  const { jobId } = useParams()
  const navigate = useNavigate()
  const editing = Boolean(jobId)

  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')

  const companies = useQuery({ queryKey: ['companies', 'active'], queryFn: api.jobs.activeCompanies })
  const batches = useQuery({ queryKey: ['batches'], queryFn: () => api.academics.list('batches') })
  const existing = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => api.jobs.byId(jobId),
    enabled: editing,
  })

  useEffect(() => {
    if (!existing.data) return
    const job = existing.data
    setForm({
      ...EMPTY,
      ...job,
      companyId: job.company?.id ?? '',
      requiredSkills: (job.requiredSkills ?? []).join(', '),
      eligibleBatchIds: job.eligibleBatchIds ?? [],
      salaryMin: job.salaryMin ?? '',
      salaryMax: job.salaryMax ?? '',
      minCgpa: job.minCgpa ?? '',
      maxBacklogs: job.maxBacklogs ?? '',
      applicationDeadline: job.applicationDeadline ?? '',
      vacancies: job.vacancies ?? '',
    })
  }, [existing.data])

  const save = useMutation({
    mutationFn: () => {
      const body = {
        ...form,
        companyId: Number(form.companyId),
        salaryMin: numberOrNull(form.salaryMin),
        salaryMax: numberOrNull(form.salaryMax),
        minCgpa: numberOrNull(form.minCgpa),
        maxBacklogs: numberOrNull(form.maxBacklogs),
        vacancies: numberOrNull(form.vacancies),
        applicationDeadline: form.applicationDeadline || null,
        requiredSkills: form.requiredSkills
          .split(',')
          .map((skill) => skill.trim())
          .filter(Boolean),
        eligibleBatchIds: form.eligibleBatchIds.map(Number),
      }
      return editing ? api.jobs.update(jobId, body) : api.jobs.create(body)
    },
    onSuccess: (job) => navigate(`/jobs/${job.id}`),
    onError: (failure) => setError(errorMessage(failure)),
  })

  if (editing && existing.isLoading) return <Spinner />

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <div className="max-w-3xl">
      <PageHeader title={editing ? 'Edit posting' : 'New posting'} subtitle="Saved as a draft until you publish it" />

      <Card>
        <Field label="Job title">
          <Input required value={form.title} onChange={update('title')} />
        </Field>

        <Field label="Company">
          <Select required value={form.companyId} onChange={update('companyId')}>
            <option value="">Choose a company</option>
            {companies.data?.map((company) => (
              <option key={company.id} value={company.id}>
                {company.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="About the role">
          <Textarea rows={5} required value={form.description} onChange={update('description')} />
        </Field>

        <div className="grid sm:grid-cols-2 gap-4">
          <Field label="What they will do">
            <Textarea value={form.responsibilities} onChange={update('responsibilities')} />
          </Field>
          <Field label="What you are looking for">
            <Textarea value={form.requirements} onChange={update('requirements')} />
          </Field>
        </div>

        <div className="grid sm:grid-cols-3 gap-4">
          <Field label="Type">
            <Select value={form.jobType} onChange={update('jobType')}>
              {JOB_TYPES.map((type) => (
                <option key={type} value={type}>
                  {titleCase(type)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Work mode">
            <Select value={form.workMode} onChange={update('workMode')}>
              {WORK_MODES.map((mode) => (
                <option key={mode} value={mode}>
                  {titleCase(mode)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Location">
            <Input value={form.location} onChange={update('location')} />
          </Field>
        </div>

        <div className="grid sm:grid-cols-3 gap-4">
          <Field label="Salary from">
            <Input type="number" value={form.salaryMin} onChange={update('salaryMin')} />
          </Field>
          <Field label="Salary to">
            <Input type="number" value={form.salaryMax} onChange={update('salaryMax')} />
          </Field>
          <Field label="Vacancies">
            <Input type="number" value={form.vacancies} onChange={update('vacancies')} />
          </Field>
        </div>

        <div className="grid sm:grid-cols-3 gap-4">
          <Field label="Minimum CGPA">
            <Input type="number" step="0.01" value={form.minCgpa} onChange={update('minCgpa')} />
          </Field>
          <Field label="Backlogs allowed">
            <Input type="number" value={form.maxBacklogs} onChange={update('maxBacklogs')} />
          </Field>
          <Field label="Apply by">
            <Input type="date" value={form.applicationDeadline} onChange={update('applicationDeadline')} />
          </Field>
        </div>

        <Field label="Skills" hint="Comma separated">
          <Input value={form.requiredSkills} onChange={update('requiredSkills')} />
        </Field>

        <Field label="Eligible batches" hint="Leave empty to open the drive to everyone">
          <select
            multiple
            className="input h-32"
            value={form.eligibleBatchIds.map(String)}
            onChange={(event) =>
              setForm({
                ...form,
                eligibleBatchIds: Array.from(event.target.selectedOptions, (option) => option.value),
              })
            }
          >
            {batches.data?.map((batch) => (
              <option key={batch.id} value={batch.id}>
                {batch.name}
              </option>
            ))}
          </select>
        </Field>

        <label className="flex items-center gap-2 text-sm mb-4">
          <input
            type="checkbox"
            checked={form.referralsEnabled}
            onChange={(event) => setForm({ ...form, referralsEnabled: event.target.checked })}
          />
          Let students ask alumni for referrals on this role
        </label>

        {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

        <div className="flex gap-2">
          <Button onClick={() => save.mutate()} loading={save.isPending}>
            {editing ? 'Save changes' : 'Create draft'}
          </Button>
          <Button variant="secondary" onClick={() => navigate(-1)}>
            Cancel
          </Button>
        </div>
      </Card>
    </div>
  )
}

const numberOrNull = (value) => (value === '' || value == null ? null : Number(value))

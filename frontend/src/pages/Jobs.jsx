import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Badge,
  Card,
  EmptyState,
  Input,
  PageHeader,
  Pagination,
  Select,
  Spinner,
} from '../components/ui'
import { api } from '../lib/api'
import { JOB_TYPES, WORK_MODES, formatDate, titleCase } from '../lib/format'

export default function Jobs() {
  const [filters, setFilters] = useState({ query: '', jobType: '', workMode: '' })
  const [page, setPage] = useState(0)

  const jobs = useQuery({
    queryKey: ['jobs', filters, page],
    queryFn: () => api.jobs.search({ ...filters, page }),
  })

  const update = (field) => (event) => {
    setFilters({ ...filters, [field]: event.target.value })
    setPage(0)
  }

  return (
    <>
      <PageHeader title="Jobs" subtitle="Roles open to your institution right now" />

      <Card className="mb-5 grid sm:grid-cols-3 gap-3">
        <Input placeholder="Search title, company or place" value={filters.query} onChange={update('query')} />
        <Select value={filters.jobType} onChange={update('jobType')}>
          <option value="">Any type</option>
          {JOB_TYPES.map((type) => (
            <option key={type} value={type}>
              {titleCase(type)}
            </option>
          ))}
        </Select>
        <Select value={filters.workMode} onChange={update('workMode')}>
          <option value="">Anywhere</option>
          {WORK_MODES.map((mode) => (
            <option key={mode} value={mode}>
              {titleCase(mode)}
            </option>
          ))}
        </Select>
      </Card>

      {jobs.isLoading && <Spinner />}
      {jobs.data?.content?.length === 0 && (
        <EmptyState title="No roles match" message="Try clearing the filters." />
      )}

      <div className="grid md:grid-cols-2 gap-4">
        {jobs.data?.content?.map((job) => (
          <Link key={job.id} to={`/jobs/${job.id}`}>
            <Card className="h-full hover:border-brand-300 transition-colors">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="text-base">{job.title}</h3>
                  <p className="text-sm text-slate-600">{job.company?.name}</p>
                </div>
                <Badge tone="brand">{job.jobTypeLabel}</Badge>
              </div>

              <p className="text-sm text-slate-500 mt-3">
                {job.location ?? 'Location not stated'} · {titleCase(job.workMode)}
              </p>
              <p className="text-sm font-medium text-slate-700 mt-1">{job.salaryLabel}</p>

              {job.requiredSkills?.length > 0 && (
                <div className="flex flex-wrap gap-1.5 mt-3">
                  {job.requiredSkills.slice(0, 4).map((skill) => (
                    <Badge key={skill}>{skill}</Badge>
                  ))}
                </div>
              )}

              <p className="text-xs text-slate-400 mt-3">
                {job.applicationCount} applied
                {job.applicationDeadline && ` · closes ${formatDate(job.applicationDeadline)}`}
              </p>
            </Card>
          </Link>
        ))}
      </div>

      <Pagination page={page} totalPages={jobs.data?.totalPages} onChange={setPage} />
    </>
  )
}

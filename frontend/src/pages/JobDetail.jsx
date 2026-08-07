import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Badge,
  Button,
  Card,
  Field,
  Modal,
  Select,
  Spinner,
  Textarea,
  statusTone,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDate, titleCase } from '../lib/format'

export default function JobDetail() {
  const { jobId } = useParams()
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()

  const [applying, setApplying] = useState(false)
  const [form, setForm] = useState({ resumeId: '', coverLetter: '' })
  const [error, setError] = useState('')

  const job = useQuery({ queryKey: ['job', jobId], queryFn: () => api.jobs.byId(jobId) })
  const resumes = useQuery({
    queryKey: ['resumes'],
    queryFn: api.resumes.list,
    enabled: hasRole('STUDENT'),
  })

  const apply = useMutation({
    mutationFn: () =>
      api.applications.apply(jobId, {
        resumeId: form.resumeId ? Number(form.resumeId) : null,
        coverLetter: form.coverLetter,
      }),
    onSuccess: () => {
      setApplying(false)
      queryClient.invalidateQueries({ queryKey: ['job', jobId] })
      queryClient.invalidateQueries({ queryKey: ['applications'] })
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  if (job.isLoading) return <Spinner />
  const detail = job.data
  if (!detail) return null

  return (
    <div className="max-w-3xl">
      <Link to="/jobs" className="text-sm link">
        ← All jobs
      </Link>

      <Card className="mt-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl">{detail.title}</h1>
            <p className="text-slate-600 mt-1">
              {detail.company?.name} · {detail.location ?? 'Location not stated'}
            </p>
            {detail.postedByName && (
              <p className="text-sm text-slate-500 mt-1">
                Posted by:{' '}
                <Link to={`/people/${detail.postedByUserId}`} className="link font-medium">
                  {detail.postedByName}
                </Link>
              </p>
            )}
          </div>
          <Badge tone={statusTone(detail.status)}>{titleCase(detail.status)}</Badge>
        </div>

        <div className="grid sm:grid-cols-3 gap-4 mt-5 text-sm">
          <Detail label="Type" value={detail.jobTypeLabel} />
          <Detail label="Work mode" value={titleCase(detail.workMode)} />
          <Detail label="Compensation" value={detail.salaryLabel} />
          <Detail label="Vacancies" value={detail.vacancies ?? 'Not stated'} />
          <Detail label="Apply by" value={formatDate(detail.applicationDeadline) || 'No deadline'} />
          <Detail label="Applicants" value={detail.applicationCount} />
        </div>

        {(detail.minCgpa || detail.maxBacklogs != null) && (
          <p className="text-sm text-slate-600 mt-4 bg-slate-50 rounded-lg p-3">
            Eligibility: {detail.minCgpa ? `CGPA ${detail.minCgpa} or above` : 'no CGPA cut-off'}
            {detail.maxBacklogs != null && `, at most ${detail.maxBacklogs} backlogs`}.
          </p>
        )}

        {hasRole('STUDENT') && (
          <div className="mt-5">
            {detail.myApplicationId ? (
              <Link to="/applications">
                <Button variant="secondary">You have applied — see your application</Button>
              </Link>
            ) : detail.eligibility?.eligible && detail.acceptingApplications ? (
              <Button onClick={() => setApplying(true)}>Apply now</Button>
            ) : (
              <div>
                <Button disabled>Cannot apply</Button>
                <ul className="text-sm text-amber-700 mt-2 list-disc pl-5">
                  {(detail.eligibility?.reasons?.length
                    ? detail.eligibility.reasons
                    : ['This job is not accepting applications.']
                  ).map((reason) => (
                    <li key={reason}>{reason}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Card>

      <Section title="About the role" body={detail.description} />
      <Section title="What you will do" body={detail.responsibilities} />
      <Section title="What we are looking for" body={detail.requirements} />

      {detail.requiredSkills?.length > 0 && (
        <Card className="mt-4">
          <h2 className="text-base mb-3">Skills</h2>
          <div className="flex flex-wrap gap-2">
            {detail.requiredSkills.map((skill) => (
              <Badge key={skill} tone="brand">
                {skill}
              </Badge>
            ))}
          </div>
        </Card>
      )}

      <Modal
        open={applying}
        title={`Apply to ${detail.title}`}
        onClose={() => setApplying(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setApplying(false)}>
              Cancel
            </Button>
            <Button onClick={() => apply.mutate()} loading={apply.isPending}>
              Submit application
            </Button>
          </>
        }
      >
        <Field label="Resume" hint="Your primary resume is used if you do not pick one">
          <Select value={form.resumeId} onChange={(event) => setForm({ ...form, resumeId: event.target.value })}>
            <option value="">Use my primary resume</option>
            {resumes.data?.map((resume) => (
              <option key={resume.id} value={resume.id}>
                {resume.label}
                {resume.primary ? ' (primary)' : ''}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Cover note" hint="Optional, but it helps">
          <Textarea
            value={form.coverLetter}
            onChange={(event) => setForm({ ...form, coverLetter: event.target.value })}
          />
        </Field>

        {error && <p className="text-sm text-red-600">{error}</p>}
      </Modal>
    </div>
  )
}

const Detail = ({ label, value }) => (
  <div>
    <p className="text-xs uppercase tracking-wide text-slate-400">{label}</p>
    <p className="font-medium text-slate-800">{value}</p>
  </div>
)

const Section = ({ title, body }) =>
  body ? (
    <Card className="mt-4">
      <h2 className="text-base mb-2">{title}</h2>
      <p className="text-sm text-slate-700 whitespace-pre-wrap">{body}</p>
    </Card>
  ) : null

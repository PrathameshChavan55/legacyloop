import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Avatar,
  Badge,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Select,
  Spinner,
  Textarea,
  statusTone,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { APPLICATION_STATUSES, formatDate, titleCase } from '../lib/format'

/** The reviewer's screen: who applied, and what may happen to each of them next. */
export default function JobApplicants() {
  const { jobId } = useParams()
  const queryClient = useQueryClient()

  const [status, setStatus] = useState('')
  const [reviewing, setReviewing] = useState(null)

  const job = useQuery({ queryKey: ['job', jobId], queryFn: () => api.jobs.byId(jobId) })
  const applicants = useQuery({
    queryKey: ['applications', 'job', jobId, status],
    queryFn: () => api.applications.forJob(jobId, { status, size: 50 }),
  })

  const review = useQuery({
    queryKey: ['application', reviewing, 'review'],
    queryFn: () => api.applications.review(reviewing),
    enabled: Boolean(reviewing),
  })

  const [decision, setDecision] = useState({ status: '', message: '', reviewerNotes: '', offeredPackage: '' })
  const [error, setError] = useState('')

  const change = useMutation({
    mutationFn: () =>
      api.applications.changeStatus(reviewing, {
        ...decision,
        offeredPackage: decision.offeredPackage === '' ? null : Number(decision.offeredPackage),
      }),
    onSuccess: () => {
      setReviewing(null)
      setDecision({ status: '', message: '', reviewerNotes: '', offeredPackage: '' })
      queryClient.invalidateQueries({ queryKey: ['applications'] })
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  if (applicants.isLoading) return <Spinner />

  return (
    <>
      <PageHeader
        title={job.data?.title ?? 'Applicants'}
        subtitle={`${applicants.data?.totalElements ?? 0} applications`}
        action={
          <Link to="/jobs/manage">
            <Button variant="secondary">Back to postings</Button>
          </Link>
        }
      />

      <Card className="mb-5 max-w-xs">
        <Select value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">Every stage</option>
          {APPLICATION_STATUSES.map((value) => (
            <option key={value} value={value}>
              {titleCase(value)}
            </option>
          ))}
        </Select>
      </Card>

      {applicants.isError && (
        <Card className="mb-4 border-red-200 bg-red-50 text-red-700">
          <p className="text-sm">{errorMessage(applicants.error)}</p>
        </Card>
      )}

      {!applicants.isError && applicants.data?.content?.length === 0 && (
        <EmptyState title="No applications yet" message="They will appear here as students apply." />
      )}

      <div className="space-y-3">
        {applicants.data?.content?.map((application) => (
          <Card key={application.id} className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Avatar name={application.applicantName} />
              <div>
                <p className="font-medium">{application.applicantName}</p>
                <p className="text-sm text-slate-500">
                  Applied {formatDate(application.appliedAt)}
                  {application.referralCount > 0 && ` · ${application.referralCount} referral requests`}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {application.resumeId && (
                <button
                  type="button"
                  onClick={() => api.resumes.viewResume(application.resumeId)}
                  className="text-xs text-primary-600 hover:underline font-medium px-2 py-1 bg-primary-50 hover:bg-primary-100 rounded inline-flex items-center gap-1 cursor-pointer transition-colors"
                >
                  📄 View Resume
                </button>
              )}
              <Badge tone={statusTone(application.status)}>{application.statusLabel}</Badge>
              <Button
                variant="secondary"
                onClick={() => {
                  setReviewing(application.id)
                  setError('')
                }}
              >
                Review
              </Button>
            </div>
          </Card>
        ))}
      </div>

      <Modal
        open={Boolean(reviewing)}
        title="Review application"
        onClose={() => setReviewing(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setReviewing(null)}>
              Close
            </Button>
            <Button onClick={() => change.mutate()} loading={change.isPending} disabled={!decision.status}>
              Update status
            </Button>
          </>
        }
      >
        {review.isLoading ? (
          <Spinner />
        ) : (
          <>
            <div className="flex items-start justify-between">
              <div>
                <p className="font-medium text-base">{review.data?.application?.applicantName}</p>
                <p className="text-sm text-slate-500">{review.data?.application?.applicantEmail}</p>
              </div>
              {review.data?.application?.resumeId && (
                <button
                  type="button"
                  onClick={() => api.resumes.viewResume(review.data.application.resumeId)}
                  className="text-xs text-primary-700 bg-primary-50 border border-primary-200 px-3 py-1.5 rounded-lg hover:bg-primary-100 font-medium inline-flex items-center gap-1.5 cursor-pointer transition-colors"
                >
                  📄 Download / View Resume
                </button>
              )}
            </div>

            <div className="grid grid-cols-3 gap-3 my-4 text-sm">
              <Snapshot label="CGPA" value={review.data?.applicantSnapshot?.cgpa ?? '—'} />
              <Snapshot label="Backlogs" value={review.data?.applicantSnapshot?.backlogs ?? '—'} />
              <Snapshot label="Batch" value={review.data?.applicantSnapshot?.batchName ?? '—'} />
            </div>

            {review.data?.application?.coverLetter && (
              <div className="mb-4">
                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Cover Letter / Note</p>
                <p className="text-sm text-slate-700 bg-slate-50 rounded-lg p-3 whitespace-pre-wrap border border-slate-100">
                  {review.data.application.coverLetter}
                </p>
              </div>
            )}

            <Field label="Move to">
              <Select
                value={decision.status}
                onChange={(event) => setDecision({ ...decision, status: event.target.value })}
              >
                <option value="">Choose a stage</option>
                {review.data?.allowedNextStatuses?.map((value) => (
                  <option key={value} value={value}>
                    {titleCase(value)}
                  </option>
                ))}
              </Select>
            </Field>

            {decision.status === 'SELECTED' && (
              <Field label="Offered package" hint="Recorded on the student's profile too">
                <Input
                  type="number"
                  value={decision.offeredPackage}
                  onChange={(event) => setDecision({ ...decision, offeredPackage: event.target.value })}
                />
              </Field>
            )}

            <Field label="Message to the applicant">
              <Textarea
                rows={2}
                value={decision.message}
                onChange={(event) => setDecision({ ...decision, message: event.target.value })}
              />
            </Field>

            <Field label="Private notes" hint="Only reviewers see this">
              <Textarea
                rows={2}
                value={decision.reviewerNotes}
                onChange={(event) => setDecision({ ...decision, reviewerNotes: event.target.value })}
              />
            </Field>

            {error && <p className="text-sm text-red-600">{error}</p>}
          </>
        )}
      </Modal>
    </>
  )
}

const Snapshot = ({ label, value }) => (
  <div className="bg-slate-50 rounded-lg p-3">
    <p className="text-xs text-slate-400">{label}</p>
    <p className="font-medium">{String(value)}</p>
  </div>
)

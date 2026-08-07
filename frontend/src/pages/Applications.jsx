import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  Modal,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  Textarea,
  statusTone,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { APPLICATION_STATUSES, formatDate, formatDateTime, titleCase } from '../lib/format'

/** A student's own applications, with the referral flow attached to each one. */
export default function Applications() {
  const queryClient = useQueryClient()
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [askingFor, setAskingFor] = useState(null)

  const applications = useQuery({
    queryKey: ['applications', 'mine', status, page],
    queryFn: () => api.applications.mine({ status, page }),
  })

  const withdraw = useMutation({
    mutationFn: api.applications.withdraw,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['applications'] }),
  })

  if (applications.isLoading) return <Spinner />

  return (
    <>
      <PageHeader title="Your applications" subtitle="Everything you have applied to" />

      <Card className="mb-5 max-w-xs">
        <Select
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPage(0)
          }}
        >
          <option value="">Every stage</option>
          {APPLICATION_STATUSES.map((value) => (
            <option key={value} value={value}>
              {titleCase(value)}
            </option>
          ))}
        </Select>
      </Card>

      {applications.data?.content?.length === 0 && (
        <EmptyState
          title="You have not applied anywhere yet"
          message="The job board is a good place to start."
          action={
            <Link to="/jobs">
              <Button>Browse jobs</Button>
            </Link>
          }
        />
      )}

      <div className="space-y-3">
        {applications.data?.content?.map((application) => (
          <Card key={application.id}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <Link to={`/jobs/${application.jobId}`} className="font-medium hover:underline">
                  {application.jobTitle}
                </Link>
                <p className="text-sm text-slate-500">
                  {application.companyName} · applied {formatDate(application.appliedAt)}
                </p>
              </div>
              <Badge tone={statusTone(application.status)}>{application.statusLabel}</Badge>
            </div>

            <div className="h-1.5 bg-slate-100 rounded-full mt-4 overflow-hidden">
              <div
                className="h-full bg-brand-500 rounded-full transition-all"
                style={{ width: `${application.progress}%` }}
              />
            </div>

            {application.statusMessage && (
              <p className="text-sm text-slate-600 mt-3">{application.statusMessage}</p>
            )}

            {application.interviewAt && (
              <p className="text-sm text-brand-700 mt-2">
                Interview {formatDateTime(application.interviewAt)}
                {application.interviewLocation && ` · ${application.interviewLocation}`}
              </p>
            )}

            <div className="flex gap-2 mt-4">
              {!application.terminal && (
                <>
                  <Button variant="secondary" onClick={() => setAskingFor(application)}>
                    Ask for a referral
                  </Button>
                  <Button variant="ghost" onClick={() => withdraw.mutate(application.id)}>
                    Withdraw
                  </Button>
                </>
              )}
            </div>
          </Card>
        ))}
      </div>

      <Pagination page={page} totalPages={applications.data?.totalPages} onChange={setPage} />

      {askingFor && <ReferralModal application={askingFor} onClose={() => setAskingFor(null)} />}
    </>
  )
}

/** Lists alumni at the company who have said they will refer, and asks the chosen ones. */
function ReferralModal({ application, onClose }) {
  const queryClient = useQueryClient()
  const [chosen, setChosen] = useState([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const referrers = useQuery({
    queryKey: ['referrers', application.id],
    queryFn: () => api.applications.referrers(application.id),
  })

  const ask = useMutation({
    mutationFn: () =>
      api.applications.requestReferrals(application.id, { message, referrerUserIds: chosen }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      onClose()
    },
    onError: (failure) => setError(errorMessage(failure)),
  })

  const toggle = (userId) =>
    setChosen((current) =>
      current.includes(userId) ? current.filter((id) => id !== userId) : [...current, userId],
    )

  return (
    <Modal
      open
      title={`Referrals for ${application.jobTitle}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => ask.mutate()} loading={ask.isPending} disabled={chosen.length === 0}>
            Send request
          </Button>
        </>
      }
    >
      {referrers.isLoading && <Spinner />}

      {referrers.data?.length === 0 && (
        <p className="text-sm text-slate-500">
          No alumni at {application.companyName} have offered to refer yet.
        </p>
      )}

      <div className="space-y-2 mb-4">
        {referrers.data?.map((person) => (
          <label
            key={person.userId}
            className="flex items-center gap-3 p-3 rounded-lg border border-slate-200 cursor-pointer"
          >
            <input
              type="checkbox"
              checked={chosen.includes(person.userId)}
              onChange={() => toggle(person.userId)}
            />
            <div>
              <p className="text-sm font-medium">{person.fullName}</p>
              <p className="text-xs text-slate-500">{person.subtitle}</p>
            </div>
          </label>
        ))}
      </div>

      <Textarea
        rows={3}
        placeholder="Say why you are a good fit — a short, specific note works best."
        value={message}
        onChange={(event) => setMessage(event.target.value)}
      />

      {error && <p className="text-sm text-red-600 mt-2">{error}</p>}
    </Modal>
  )
}

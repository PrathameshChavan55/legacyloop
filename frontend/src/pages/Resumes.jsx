import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  PageHeader,
  Spinner,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { formatDate } from '../lib/format'

/**
 * Resume list and analysis on one page.
 *
 * The analysis is a row with a status, so this polls it every two seconds until it is finished —
 * that is why the API returns an id rather than the result.
 */
export default function Resumes() {
  const queryClient = useQueryClient()
  const fileInput = useRef(null)

  const [error, setError] = useState('')
  const [analysisId, setAnalysisId] = useState(null)

  const resumes = useQuery({ queryKey: ['resumes'], queryFn: api.resumes.list })

  const analysis = useQuery({
    queryKey: ['analysis', analysisId],
    queryFn: () => api.ai.analysis(analysisId),
    enabled: Boolean(analysisId),
    refetchInterval: (query) => (query.state.data?.status === 'PENDING' ? 2000 : false),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['resumes'] })

  const upload = useMutation({
    mutationFn: (file) => api.resumes.upload(file),
    onSuccess: invalidate,
    onError: (failure) => setError(errorMessage(failure)),
  })

  const makePrimary = useMutation({ mutationFn: api.resumes.makePrimary, onSuccess: invalidate })
  const remove = useMutation({ mutationFn: api.resumes.remove, onSuccess: invalidate })

  const analyse = useMutation({
    mutationFn: (resumeId) => api.ai.analyse(resumeId, null),
    onSuccess: (accepted) => setAnalysisId(accepted.analysisId),
    onError: (failure) => setError(errorMessage(failure)),
  })

  if (resumes.isLoading) return <Spinner />

  return (
    <>
      <PageHeader
        title="Resumes"
        subtitle="Up to five, one of them primary"
        action={
          <>
            <input
              ref={fileInput}
              type="file"
              accept=".pdf,.doc,.docx,.txt"
              hidden
              onChange={(event) => {
                if (event.target.files?.[0]) upload.mutate(event.target.files[0])
                event.target.value = ''
              }}
            />
            <Button onClick={() => fileInput.current?.click()} loading={upload.isPending}>
              Upload a resume
            </Button>
          </>
        }
      />

      {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

      {resumes.data?.length === 0 && (
        <EmptyState
          title="No resumes yet"
          message="Upload a PDF and we can check it against a job for you."
        />
      )}

      <div className="space-y-3">
        {resumes.data?.map((resume) => (
          <Card key={resume.id} className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-medium flex items-center gap-2">
                {resume.label}
                {resume.primary && <Badge tone="brand">Primary</Badge>}
              </p>
              <p className="text-sm text-slate-500">
                {resume.sizeLabel} · uploaded {formatDate(resume.uploadedAt)}
                {!resume.textExtracted && ' · text could not be read'}
              </p>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                onClick={() => analyse.mutate(resume.id)}
                loading={analyse.isPending}
                disabled={!resume.textExtracted}
              >
                Analyse
              </Button>
              <Button variant="secondary" onClick={() => api.resumes.viewResume(resume.id)}>
                Download / View
              </Button>
              {!resume.primary && (
                <Button variant="ghost" onClick={() => makePrimary.mutate(resume.id)}>
                  Make primary
                </Button>
              )}
              <Button variant="ghost" onClick={() => remove.mutate(resume.id)}>
                Delete
              </Button>
            </div>
          </Card>
        ))}
      </div>

      {analysisId && (
        <Card className="mt-6">
          <h2 className="text-base mb-3">Analysis</h2>

          {analysis.data?.status === 'PENDING' && <Spinner label="Reading your resume" />}
          {analysis.data?.status === 'FAILED' && (
            <p className="text-sm text-red-600">{analysis.data.errorMessage}</p>
          )}

          {analysis.data?.status === 'COMPLETED' && (
            <>
              <div className="flex items-center gap-4 mb-4">
                <div className="w-16 h-16 rounded-full bg-brand-50 text-brand-700 grid place-items-center text-xl font-semibold">
                  {analysis.data.score}
                </div>
                <p className="text-sm text-slate-700">{analysis.data.summary}</p>
              </div>

              <div className="grid sm:grid-cols-3 gap-4">
                <FindingList title="Strengths" items={analysis.data.strengths} tone="text-green-700" />
                <FindingList title="Gaps" items={analysis.data.gaps} tone="text-amber-700" />
                <FindingList title="Suggestions" items={analysis.data.suggestions} tone="text-slate-700" />
              </div>
            </>
          )}
        </Card>
      )}
    </>
  )
}

const FindingList = ({ title, items, tone }) =>
  items?.length ? (
    <div>
      <p className="text-sm font-medium mb-2">{title}</p>
      <ul className={`text-sm space-y-1 list-disc pl-4 ${tone}`}>
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </div>
  ) : null

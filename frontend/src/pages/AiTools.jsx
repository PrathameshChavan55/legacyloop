import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import {
  Button,
  Card,
  Field,
  Input,
  PageHeader,
  Select,
  Spinner,
  Tabs,
  Textarea,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'

/** The resume writer and the interview practice, which are the same shape: a form and a result. */
export default function AiTools() {
  const [tab, setTab] = useState('builder')

  return (
    <>
      <PageHeader
        title="AI tools"
        subtitle="Draft resume content and practise for interviews"
      />

      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { value: 'builder', label: 'Resume writer' },
          { value: 'questions', label: 'Practice questions' },
          { value: 'feedback', label: 'Answer feedback' },
        ]}
      />

      {tab === 'builder' && <ResumeBuilder />}
      {tab === 'questions' && <InterviewQuestions />}
      {tab === 'feedback' && <AnswerFeedback />}
    </>
  )
}

function ResumeBuilder() {
  const [form, setForm] = useState({ targetRole: '', experience: '', skills: '', education: '', projects: '' })
  const build = useMutation({ mutationFn: () => api.ai.buildResume(form) })
  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <div className="grid lg:grid-cols-2 gap-6">
      <Card>
        <Field label="Target role">
          <Input required value={form.targetRole} onChange={update('targetRole')} />
        </Field>
        <Field label="Experience">
          <Textarea value={form.experience} onChange={update('experience')} />
        </Field>
        <Field label="Skills" hint="Comma separated">
          <Input value={form.skills} onChange={update('skills')} />
        </Field>
        <Field label="Education">
          <Textarea rows={2} value={form.education} onChange={update('education')} />
        </Field>
        <Field label="Projects">
          <Textarea value={form.projects} onChange={update('projects')} />
        </Field>

        <Button onClick={() => build.mutate()} loading={build.isPending} disabled={!form.targetRole}>
          Draft content
        </Button>
        {build.isError && <p className="text-sm text-red-600 mt-3">{errorMessage(build.error)}</p>}
      </Card>

      <Card>
        {build.isPending && <Spinner label="Writing" />}
        {!build.data && !build.isPending && (
          <p className="text-sm text-slate-500">Fill in the form and the draft appears here.</p>
        )}

        {build.data && (
          <>
            <h3 className="text-base mb-2">Summary</h3>
            <p className="text-sm text-slate-700">{build.data.summary}</p>

            <h3 className="text-base mt-5 mb-2">Bullet points</h3>
            <ul className="text-sm text-slate-700 list-disc pl-5 space-y-1">
              {build.data.bulletPoints?.map((point) => (
                <li key={point}>{point}</li>
              ))}
            </ul>

            <h3 className="text-base mt-5 mb-2">Worth doing</h3>
            <ul className="text-sm text-slate-600 list-disc pl-5 space-y-1">
              {build.data.skillSuggestions?.map((suggestion) => (
                <li key={suggestion}>{suggestion}</li>
              ))}
            </ul>
          </>
        )}
      </Card>
    </div>
  )
}

function InterviewQuestions() {
  const [form, setForm] = useState({ role: '', company: '', difficulty: 'medium', focusAreas: '' })
  const generate = useMutation({ mutationFn: () => api.ai.interviewQuestions(form) })
  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <div className="grid lg:grid-cols-2 gap-6">
      <Card>
        <Field label="Role">
          <Input required value={form.role} onChange={update('role')} />
        </Field>
        <Field label="Company" hint="Optional">
          <Input value={form.company} onChange={update('company')} />
        </Field>
        <Field label="Difficulty">
          <Select value={form.difficulty} onChange={update('difficulty')}>
            <option value="easy">Easy</option>
            <option value="medium">Medium</option>
            <option value="hard">Hard</option>
          </Select>
        </Field>
        <Field label="Focus areas" hint="Comma separated">
          <Input value={form.focusAreas} onChange={update('focusAreas')} />
        </Field>

        <Button onClick={() => generate.mutate()} loading={generate.isPending} disabled={!form.role}>
          Generate questions
        </Button>
      </Card>

      <Card>
        {generate.isPending && <Spinner label="Thinking" />}
        {!generate.data && !generate.isPending && (
          <p className="text-sm text-slate-500">Questions appear here.</p>
        )}

        <div className="space-y-4">
          {generate.data?.questions?.map((question, index) => (
            <div key={question.question} className="border-b border-slate-100 pb-3 last:border-0">
              <p className="text-sm font-medium">
                {index + 1}. {question.question}
              </p>
              <p className="text-xs text-slate-500 mt-1">
                {question.category}
                {question.hint && ` · ${question.hint}`}
              </p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}

function AnswerFeedback() {
  const [form, setForm] = useState({ question: '', answer: '' })
  const score = useMutation({ mutationFn: () => api.ai.interviewFeedback(form) })
  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <div className="grid lg:grid-cols-2 gap-6">
      <Card>
        <Field label="The question you were asked">
          <Textarea rows={2} value={form.question} onChange={update('question')} />
        </Field>
        <Field label="Your answer">
          <Textarea rows={8} value={form.answer} onChange={update('answer')} />
        </Field>

        <Button
          onClick={() => score.mutate()}
          loading={score.isPending}
          disabled={!form.question || !form.answer}
        >
          Get feedback
        </Button>
      </Card>

      <Card>
        {score.isPending && <Spinner label="Reading your answer" />}
        {!score.data && !score.isPending && (
          <p className="text-sm text-slate-500">Feedback appears here.</p>
        )}

        {score.data && (
          <>
            <div className="flex items-center gap-4 mb-4">
              <div className="w-14 h-14 rounded-full bg-brand-50 text-brand-700 grid place-items-center text-lg font-semibold">
                {score.data.score}
              </div>
              <p className="font-medium">{score.data.verdict}</p>
            </div>

            <p className="text-sm font-medium mb-1">What worked</p>
            <ul className="text-sm text-green-700 list-disc pl-5 mb-4">
              {score.data.strengths?.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>

            <p className="text-sm font-medium mb-1">What to change</p>
            <ul className="text-sm text-amber-700 list-disc pl-5 mb-4">
              {score.data.improvements?.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>

            <p className="text-sm font-medium mb-1">A stronger shape</p>
            <p className="text-sm text-slate-700">{score.data.modelAnswer}</p>
          </>
        )}
      </Card>
    </div>
  )
}

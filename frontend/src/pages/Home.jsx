import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Link } from 'react-router-dom'
import { Button, Card, ErrorState, PageHeader, Spinner } from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'
import { titleCase } from '../lib/format'

/**
 * The dashboard. The backend decides which numbers belong to this viewer, so this page renders
 * whatever it is given rather than branching on the role itself.
 */
export default function Home() {
  const { user, hasRole } = useAuth()
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: api.analytics.dashboard })

  if (dashboard.isLoading) return <Spinner />
  if (dashboard.isError) {
    return <ErrorState error={errorMessage(dashboard.error)} onRetry={dashboard.refetch} />
  }

  const { headline = {}, applicationsByStatus = [], topCompanies = [], applicationsOverTime = [], highlights = [] } =
    dashboard.data ?? {}

  return (
    <>
      <PageHeader
        title={`Hello, ${user?.firstName ?? 'there'}`}
        subtitle="Here is where things stand today"
        action={
          hasRole('INSTITUTION_STAFF', 'PLATFORM_ADMIN') ? (
            <a href={api.analytics.exportUrl('placement-register')}>
              <Button variant="secondary">Download placement register</Button>
            </a>
          ) : (
            <Link to="/jobs">
              <Button>Browse jobs</Button>
            </Link>
          )
        }
      />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {Object.entries(headline).map(([label, value]) => (
          <Card key={label}>
            <p className="text-xs uppercase tracking-wide text-slate-500">{splitCamel(label)}</p>
            <p className="text-3xl font-semibold text-slate-900 mt-1">{value}</p>
          </Card>
        ))}
      </div>

      {highlights.length > 0 && (
        <Card className="mb-6 bg-brand-50 border-brand-100">
          <ul className="text-sm text-brand-900 space-y-1">
            {highlights.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </Card>
      )}

      <div className="grid lg:grid-cols-2 gap-6">
        <Card>
          <h2 className="text-base mb-4">Applications over the last fortnight</h2>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={applicationsOverTime}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} tickFormatter={(value) => value.slice(5)} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="count" fill="#6366f1" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card>
          <h2 className="text-base mb-4">By stage</h2>
          {applicationsByStatus.length === 0 ? (
            <p className="text-sm text-slate-500">Nothing to show yet.</p>
          ) : (
            <ul className="space-y-2">
              {applicationsByStatus.map((entry) => (
                <li key={entry.label} className="flex items-center justify-between text-sm">
                  <span className="text-slate-600">{titleCase(entry.label)}</span>
                  <span className="font-medium">{entry.count}</span>
                </li>
              ))}
            </ul>
          )}

          {topCompanies.length > 0 && (
            <>
              <h2 className="text-base mt-6 mb-3">Top employers</h2>
              <ul className="space-y-2">
                {topCompanies.map((entry) => (
                  <li key={entry.label} className="flex items-center justify-between text-sm">
                    <span className="text-slate-600">{entry.label}</span>
                    <span className="font-medium">{entry.count} roles</span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </Card>
      </div>
    </>
  )
}

/** openJobs becomes "Open jobs" — the backend sends camelCase keys. */
const splitCamel = (value) =>
  value.replace(/([A-Z])/g, ' $1').replace(/^./, (character) => character.toUpperCase())

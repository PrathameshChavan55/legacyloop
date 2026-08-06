import { Link } from 'react-router-dom'
import { Button } from '../components/ui'

const FEATURES = [
  ['One place for the placement season', 'Drives, eligibility, applications and offers, tracked end to end.'],
  ['Alumni who can actually help', 'Ask someone already inside the company to refer you.'],
  ['A resume that gets read', 'Have it checked against the job before you send it.'],
]

export default function Landing() {
  return (
    <div className="min-h-screen">
      <header className="flex items-center justify-between px-6 py-4 max-w-5xl mx-auto">
        <div className="flex items-center gap-2">
          <span className="w-8 h-8 rounded-lg bg-brand-600 text-white grid place-items-center font-bold">L</span>
          <span className="font-semibold">LegacyLoop</span>
        </div>
        <div className="flex gap-2">
          <Link to="/login">
            <Button variant="ghost">Sign in</Button>
          </Link>
          <Link to="/register">
            <Button>Create an account</Button>
          </Link>
        </div>
      </header>

      <section className="max-w-3xl mx-auto text-center px-6 py-20">
        <h1 className="text-4xl sm:text-5xl leading-tight">
          Your college network, working for your career.
        </h1>
        <p className="mt-5 text-lg text-slate-600">
          LegacyLoop connects students with the alumni who came before them, and gives the placement
          cell one view of the whole season.
        </p>
        <Link to="/register">
          <Button className="mt-8 px-6 py-3">Get started</Button>
        </Link>
      </section>

      <section className="max-w-5xl mx-auto grid md:grid-cols-3 gap-6 px-6 pb-24">
        {FEATURES.map(([title, description]) => (
          <div key={title} className="card p-6">
            <h3 className="text-base">{title}</h3>
            <p className="text-sm text-slate-600 mt-2">{description}</p>
          </div>
        ))}
      </section>
    </div>
  )
}

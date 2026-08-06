import { Link } from 'react-router-dom'

/** The frame every signed-out screen shares: brand on the left, one card in the middle. */
export default function AuthShell({ title, subtitle, children, footer }) {
  return (
    <div className="min-h-screen grid lg:grid-cols-2">
      <div className="hidden lg:flex flex-col justify-center px-16 bg-brand-600 text-white">
        <Link to="/welcome" className="flex items-center gap-2 mb-8">
          <span className="w-9 h-9 rounded-lg bg-white text-brand-600 grid place-items-center font-bold">
            L
          </span>
          <span className="text-xl font-semibold">LegacyLoop</span>
        </Link>
        <h2 className="text-3xl font-semibold text-white leading-snug max-w-sm">
          Where students, alumni and placement teams stay connected.
        </h2>
        <p className="mt-4 text-brand-100 max-w-sm">
          Find roles, ask an alumnus for a referral, and keep the whole placement season in one place.
        </p>
      </div>

      <div className="flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <Link to="/welcome" className="lg:hidden flex items-center gap-2 mb-8 justify-center">
            <span className="w-8 h-8 rounded-lg bg-brand-600 text-white grid place-items-center font-bold">
              L
            </span>
            <span className="font-semibold">LegacyLoop</span>
          </Link>

          <h1 className="text-2xl">{title}</h1>
          {subtitle && <p className="text-sm text-slate-500 mt-1 mb-6">{subtitle}</p>}
          {children}
          {footer && <div className="mt-6 text-sm text-slate-600 text-center">{footer}</div>}
        </div>
      </div>
    </div>
  )
}

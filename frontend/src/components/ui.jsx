import { Loader2, X } from 'lucide-react'
import { cn, initials } from '../lib/format'

/**
 * Every shared UI primitive, in one file.
 *
 * The original had twelve component files under components/ui, most of them a div with two class
 * names. These are the ones that actually repeat across screens; anything used once lives in the
 * page that uses it.
 */

const BUTTON_STYLES = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700',
  secondary: 'bg-white text-slate-700 border border-slate-300 hover:bg-slate-50',
  ghost: 'text-slate-600 hover:bg-slate-100',
  danger: 'bg-red-600 text-white hover:bg-red-700',
}

export function Button({ variant = 'primary', loading, className, children, ...props }) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium',
        'transition-colors disabled:opacity-50 disabled:cursor-not-allowed',
        BUTTON_STYLES[variant],
        className,
      )}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading && <Loader2 className="w-4 h-4 animate-spin" />}
      {children}
    </button>
  )
}

export function Card({ className, children, ...props }) {
  return (
    <div className={cn('card p-5', className)} {...props}>
      {children}
    </div>
  )
}

export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 mb-6">
      <div>
        <h1 className="text-2xl">{title}</h1>
        {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function Field({ label, error, hint, children }) {
  return (
    <div className="mb-4">
      {label && <label className="label">{label}</label>}
      {children}
      {hint && !error && <p className="text-xs text-slate-500 mt-1">{hint}</p>}
      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
    </div>
  )
}

export const Input = (props) => <input className="input" {...props} />
export const Textarea = (props) => <textarea className="input" rows={4} {...props} />

export function Select({ children, ...props }) {
  return (
    <select className="input" {...props}>
      {children}
    </select>
  )
}

const BADGE_TONES = {
  neutral: 'bg-slate-100 text-slate-700',
  brand: 'bg-brand-50 text-brand-700',
  green: 'bg-green-100 text-green-700',
  amber: 'bg-amber-100 text-amber-700',
  red: 'bg-red-100 text-red-700',
}

export function Badge({ tone = 'neutral', children }) {
  return (
    <span className={cn('inline-block rounded-full px-2.5 py-0.5 text-xs font-medium', BADGE_TONES[tone])}>
      {children}
    </span>
  )
}

/** Status colours live here so every screen agrees on what "selected" looks like. */
export const statusTone = (status) =>
  ({
    ACTIVE: 'green',
    SELECTED: 'green',
    ACCEPTED: 'green',
    OPEN: 'green',
    SUSPENDED: 'red',
    REJECTED: 'red',
    FAILED: 'red',
    DECLINED: 'red',
    PENDING_APPROVAL: 'amber',
    PENDING_VERIFICATION: 'amber',
    PENDING: 'amber',
    REQUESTED: 'amber',
    DRAFT: 'amber',
  })[status] ?? 'neutral'

export function Avatar({ name, url, size = 40 }) {
  if (url) {
    return (
      <img
        src={url}
        alt={name}
        style={{ width: size, height: size }}
        className="rounded-full object-cover bg-slate-200"
      />
    )
  }
  return (
    <div
      style={{ width: size, height: size, fontSize: size / 2.6 }}
      className="rounded-full bg-brand-100 text-brand-700 font-semibold flex items-center justify-center"
    >
      {initials(name)}
    </div>
  )
}

export function Spinner({ label = 'Loading' }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-slate-500 text-sm">
      <Loader2 className="w-5 h-5 animate-spin" />
      {label}
    </div>
  )
}

export function EmptyState({ title, message, action }) {
  return (
    <div className="text-center py-12">
      <p className="font-medium text-slate-700">{title}</p>
      {message && <p className="text-sm text-slate-500 mt-1 max-w-md mx-auto">{message}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function ErrorState({ error, onRetry }) {
  return (
    <div className="text-center py-12">
      <p className="font-medium text-red-700">{error}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}

export function Modal({ open, title, onClose, children, footer }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="card w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <h2 className="text-lg">{title}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600" aria-label="Close">
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-5">{children}</div>
        {footer && <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-3">{footer}</div>}
      </div>
    </div>
  )
}

/** Works with the backend's page envelope: `{page, totalPages}`. */
export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  return (
    <div className="flex items-center justify-center gap-3 py-6">
      <Button variant="secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Previous
      </Button>
      <span className="text-sm text-slate-500">
        Page {page + 1} of {totalPages}
      </span>
      <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next
      </Button>
    </div>
  )
}

export function Tabs({ tabs, active, onChange }) {
  return (
    <div className="flex gap-1 border-b border-slate-200 mb-5 overflow-x-auto">
      {tabs.map((tab) => (
        <button
          key={tab.value}
          onClick={() => onChange(tab.value)}
          className={cn(
            'px-4 py-2 text-sm font-medium border-b-2 -mb-px whitespace-nowrap',
            active === tab.value
              ? 'border-brand-600 text-brand-700'
              : 'border-transparent text-slate-500 hover:text-slate-700',
          )}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}

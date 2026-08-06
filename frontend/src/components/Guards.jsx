import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { Spinner } from './ui'

/**
 * Route guards.
 *
 * Two components rather than the original's guard directory: "are you signed in" and "are you the
 * right role" are the only two questions any route asks.
 */

export function RequireAuth({ children }) {
  const { isSignedIn, loading, user } = useAuth()
  const location = useLocation()

  if (loading) return <Spinner label="Checking your session" />
  if (!isSignedIn) return <Navigate to="/login" state={{ from: location.pathname }} replace />

  // An admin-issued temporary password is good for exactly one thing: changing it.
  if (user?.mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />
  }
  return children
}

export function RequireRole({ roles, children }) {
  const { hasRole } = useAuth()
  if (!hasRole(...roles)) {
    return (
      <div className="text-center py-16">
        <h1 className="text-xl">Not your page</h1>
        <p className="text-sm text-slate-500 mt-2">
          This area is for {roles.map((role) => role.toLowerCase().replaceAll('_', ' ')).join(' and ')}.
        </p>
      </div>
    )
  }
  return children
}

export function RequirePremium({ children }) {
  const { user } = useAuth()
  if (!user?.premium) {
    return <Navigate to="/premium" replace />
  }
  return children
}

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, tokens } from './api'

/**
 * Who is signed in, for the whole app.
 *
 * One context replaces the original's Redux store, two slices, the store configuration and the
 * typed hooks around it. Signed-in state is a single object that changes on sign-in and sign-out;
 * everything else the app knows comes from the server through React Query, which already caches.
 */
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // A stored token might be stale, so on first load we ask the server who it belongs to.
  useEffect(() => {
    if (!tokens.read()?.accessToken) {
      setLoading(false)
      return
    }
    api.auth
      .me()
      .then(setUser)
      .catch(() => tokens.clear())
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email, password) => {
    const session = await api.auth.login({ email, password })
    tokens.write({ accessToken: session.accessToken, refreshToken: session.refreshToken })
    setUser(session.user)
    return session
  }, [])

  const logout = useCallback(async () => {
    const stored = tokens.read()
    if (stored?.refreshToken) await api.auth.logout(stored.refreshToken).catch(() => {})
    tokens.clear()
    setUser(null)
  }, [])

  const refreshUser = useCallback(() => api.auth.me().then(setUser), [])

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      logout,
      refreshUser,
      isSignedIn: Boolean(user),
      hasRole: (...roles) => roles.some((role) => user?.roles?.includes(`ROLE_${role}`)),
    }),
    [user, loading, login, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}

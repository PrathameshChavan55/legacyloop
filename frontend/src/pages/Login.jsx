import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import AuthShell from '../components/AuthShell'
import { Button, Field, Input } from '../components/ui'
import { errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const session = await login(form.email, form.password)
      // A temporary password is only good for setting a real one.
      navigate(session.mustChangePassword ? '/change-password' : (location.state?.from ?? '/'), {
        replace: true,
      })
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Welcome back"
      subtitle="Sign in to your LegacyLoop account"
      footer={
        <>
          New here? <Link className="link" to="/register">Create an account</Link>
        </>
      }
    >
      <form onSubmit={submit}>
        <Field label="Email">
          <Input
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })}
          />
        </Field>

        <Field label="Password">
          <Input
            type="password"
            required
            autoComplete="current-password"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
          />
        </Field>

        {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

        <Button type="submit" loading={busy} className="w-full">
          Sign in
        </Button>

        <div className="text-center mt-4">
          <Link className="text-sm link" to="/forgot-password">
            Forgotten your password?
          </Link>
        </div>
      </form>
    </AuthShell>
  )
}

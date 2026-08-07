import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthShell from '../components/AuthShell'
import { Button, Field, Input } from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'

/** Reached from the menu, or forced when an admin has issued a temporary password. */
export default function ChangePassword() {
  const navigate = useNavigate()
  const { user, refreshUser } = useAuth()

  const [form, setForm] = useState({ currentPassword: '', newPassword: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.auth.changePassword(form)
      await refreshUser()
      navigate('/', { replace: true })
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Change your password"
      subtitle={
        user?.mustChangePassword
          ? 'Your account was created with a temporary password. Choose your own to continue.'
          : 'Changing your password signs you out everywhere else.'
      }
    >
      <form onSubmit={submit}>
        <Field label="Current password">
          <Input
            type="password"
            required
            value={form.currentPassword}
            onChange={(event) => setForm({ ...form, currentPassword: event.target.value })}
          />
        </Field>

        <Field label="New password" hint="Upper case, lower case, a digit and a symbol, 8 or more">
          <Input
            type="password"
            required
            value={form.newPassword}
            onChange={(event) => setForm({ ...form, newPassword: event.target.value })}
          />
        </Field>

        {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

        <Button type="submit" loading={busy} className="w-full">
          Change password
        </Button>
      </form>
    </AuthShell>
  )
}

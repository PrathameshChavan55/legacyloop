import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthShell from '../components/AuthShell'
import { Button, Field, Input } from '../components/ui'
import { api, errorMessage } from '../lib/api'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const navigate = useNavigate()

  const [token, setToken] = useState(params.get('token') ?? '')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.auth.resetPassword({ token, newPassword })
      navigate('/login', { replace: true })
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Choose a new password"
      footer={<Link className="link" to="/login">Back to sign in</Link>}
    >
      <form onSubmit={submit}>
        {!params.get('token') && (
          <Field label="Reset token" hint="From the email we sent you">
            <Input required value={token} onChange={(event) => setToken(event.target.value)} />
          </Field>
        )}

        <Field
          label="New password"
          hint="At least 8 characters with upper case, lower case, a digit and a symbol"
        >
          <Input
            type="password"
            required
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
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

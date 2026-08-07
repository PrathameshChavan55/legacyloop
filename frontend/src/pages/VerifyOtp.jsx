import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import AuthShell from '../components/AuthShell'
import { Button, Field, Input } from '../components/ui'
import { api, errorMessage } from '../lib/api'

export default function VerifyOtp() {
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState(location.state?.email ?? '')
  const [otp, setOtp] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const result = await api.auth.verifyOtp({ email, otp })
      if (result.canSignIn) {
        navigate('/login', { replace: true })
      } else {
        setMessage(result.message)
      }
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  const resend = async () => {
    setError('')
    try {
      await api.auth.resendOtp(email)
      setMessage('A new code is on its way.')
    } catch (failure) {
      setError(errorMessage(failure))
    }
  }

  return (
    <AuthShell
      title="Check your email"
      subtitle="Enter the six-digit code we sent you"
      footer={<Link className="link" to="/login">Back to sign in</Link>}
    >
      <form onSubmit={submit}>
        <Field label="Email">
          <Input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </Field>

        <Field label="Verification code">
          <Input
            required
            inputMode="numeric"
            maxLength={6}
            placeholder="000000"
            className="input tracking-[0.4em] text-center text-lg"
            value={otp}
            onChange={(event) => setOtp(event.target.value.replace(/\D/g, ''))}
          />
        </Field>

        {message && <p className="text-sm text-green-700 mb-4">{message}</p>}
        {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

        <Button type="submit" loading={busy} className="w-full">
          Verify
        </Button>

        <button type="button" onClick={resend} className="text-sm link block mx-auto mt-4">
          Send another code
        </button>
      </form>
    </AuthShell>
  )
}

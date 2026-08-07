import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthShell from '../components/AuthShell'
import { Button, Field, Input } from '../components/ui'
import { api, errorMessage } from '../lib/api'

export default function ForgotPassword() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [step, setStep] = useState(1) // 1: Send OTP, 2: Reset Password, 3: Success
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  const handleSendOtp = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await api.auth.forgotPassword(email)
      setStep(2)
      setMessage(`A 6-digit OTP has been sent to ${email}.`)
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  const handleResetPassword = async (event) => {
    event.preventDefault()
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match')
      return
    }
    setBusy(true)
    setError('')
    try {
      await api.auth.resetPassword({ email, otp, newPassword })
      setStep(3)
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title={step === 3 ? 'Password Reset Complete' : 'Reset your password'}
      subtitle={
        step === 1
          ? 'Enter your registered email to receive an OTP'
          : step === 2
          ? 'Verify OTP and choose a new password'
          : 'Your password has been updated'
      }
      footer={<Link className="link" to="/login">Back to sign in</Link>}
    >
      {step === 1 && (
        <form onSubmit={handleSendOtp}>
          <Field label="Email address">
            <Input
              type="email"
              required
              placeholder="name@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </Field>
          {error && <p className="text-sm text-red-600 mb-4">{error}</p>}
          <Button type="submit" loading={busy} className="w-full">
            Send Verification OTP
          </Button>
        </form>
      )}

      {step === 2 && (
        <form onSubmit={handleResetPassword}>
          {message && (
            <div className="mb-4 p-3 bg-brand-50 border border-brand-200 text-brand-700 text-xs rounded-lg">
              {message}
            </div>
          )}

          <Field label="Email address">
            <div className="flex gap-2 items-center">
              <Input type="email" disabled value={email} className="bg-slate-50" />
              <Button
                type="button"
                variant="ghost"
                className="text-xs text-slate-500 whitespace-nowrap"
                onClick={() => setStep(1)}
              >
                Change
              </Button>
            </div>
          </Field>

          <Field label="6-Digit OTP Code" hint="Check your inbox or service logs">
            <Input
              required
              maxLength={6}
              placeholder="e.g. 123456"
              value={otp}
              onChange={(event) => setOtp(event.target.value)}
            />
          </Field>

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

          <Field label="Confirm new password">
            <Input
              type="password"
              required
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
          </Field>

          {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

          <div className="space-y-2">
            <Button type="submit" loading={busy} className="w-full">
              Verify OTP & Reset Password
            </Button>

            <button
              type="button"
              className="w-full text-xs text-slate-500 hover:text-slate-700 py-1"
              onClick={handleSendOtp}
              disabled={busy}
            >
              Resend OTP Code
            </button>
          </div>
        </form>
      )}

      {step === 3 && (
        <div className="text-center space-y-4">
          <p className="text-sm text-slate-600">
            Your password has been reset successfully. You can now sign in with your new password.
          </p>
          <Button className="w-full" onClick={() => navigate('/login')}>
            Sign In Now
          </Button>
        </div>
      )}
    </AuthShell>
  )
}

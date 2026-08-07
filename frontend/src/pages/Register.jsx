import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthShell from '../components/AuthShell'
import { Button, Field, Input, Select } from '../components/ui'
import { api, errorMessage } from '../lib/api'

/**
 * The label and the rule for a student's identifier come from the chosen institution rather than
 * being hard-coded, which is what lets a second college use the same sign-up form.
 */
export default function Register() {
  const navigate = useNavigate()
  const institutions = useQuery({ queryKey: ['branding'], queryFn: api.institutions.branding })

  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    role: 'ROLE_STUDENT',
    institutionId: '',
    studentIdentifier: '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const chosen = institutions.data?.find((item) => String(item.id) === String(form.institutionId))
  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.auth.register({ ...form, institutionId: Number(form.institutionId) })
      navigate('/verify', { state: { email: form.email } })
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Create your account"
      subtitle="Students and alumni can sign up here"
      footer={
        <>
          Already have an account? <Link className="link" to="/login">Sign in</Link>
        </>
      }
    >
      <form onSubmit={submit}>
        <div className="grid grid-cols-2 gap-3">
          <Field label="First name">
            <Input required value={form.firstName} onChange={update('firstName')} />
          </Field>
          <Field label="Last name">
            <Input required value={form.lastName} onChange={update('lastName')} />
          </Field>
        </div>

        <Field label="Email">
          <Input type="email" required value={form.email} onChange={update('email')} />
        </Field>

        <Field label="Mobile" hint="Optional, 10 digits">
          <Input value={form.phone} onChange={update('phone')} />
        </Field>

        <Field label="I am a">
          <Select value={form.role} onChange={update('role')}>
            <option value="ROLE_STUDENT">Student</option>
            <option value="ROLE_ALUMNI">Alumnus</option>
          </Select>
        </Field>

        <Field label="Institution">
          <Select required value={form.institutionId} onChange={update('institutionId')}>
            <option value="">Choose your institution</option>
            {institutions.data?.map((institution) => (
              <option key={institution.id} value={institution.id}>
                {institution.name}
              </option>
            ))}
          </Select>
        </Field>

        {form.role === 'ROLE_STUDENT' && chosen && (
          <Field label={chosen.identifierLabel} hint={chosen.identifierPattern && 'As printed on your ID card'}>
            <Input
              required
              value={form.studentIdentifier}
              onChange={update('studentIdentifier')}
            />
          </Field>
        )}

        <Field
          label="Password"
          hint="At least 8 characters with upper case, lower case, a digit and a symbol"
        >
          <Input type="password" required value={form.password} onChange={update('password')} />
        </Field>

        {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

        <Button type="submit" loading={busy} className="w-full">
          Create account
        </Button>
      </form>
    </AuthShell>
  )
}

import Placeholder from '../components/Placeholder'

/**
 * VerifyOtp — owner: Member 1.
 *
 * Six-digit code from the registration email
 *
 * Build it here. Everything you need already exists:
 *   import { api } from '../lib/api'                  the endpoints
 *   import { useAuth } from '../lib/auth'              who is signed in
 *   import { useToast } from '../lib/toast'            success and error messages
 *   import { Card, Button, Spinner } from '../components/ui'
 *   import { useQuery, useMutation } from '@tanstack/react-query'
 *
 * Handle four states: loading, error, empty, data. See docs/PATTERNS.md.
 */
export default function VerifyOtp() {
  return <Placeholder title="VerifyOtp" owner="Member 1" notes="Six-digit code from the registration email" />
}

import Placeholder from '../components/Placeholder'

/**
 * PublicProfile — owner: Member 2.
 *
 * Someone else's profile
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
export default function PublicProfile() {
  return <Placeholder title="PublicProfile" owner="Member 2" notes="Someone else's profile" />
}

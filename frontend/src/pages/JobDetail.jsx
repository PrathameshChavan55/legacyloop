import Placeholder from '../components/Placeholder'

/**
 * JobDetail — owner: Member 3.
 *
 * One job, with the apply button
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
export default function JobDetail() {
  return <Placeholder title="JobDetail" owner="Member 3" notes="One job, with the apply button" />
}

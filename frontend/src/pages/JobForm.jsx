import Placeholder from '../components/Placeholder'

/**
 * JobForm — owner: Member 3.
 *
 * Create or edit a job post
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
export default function JobForm() {
  return <Placeholder title="JobForm" owner="Member 3" notes="Create or edit a job post" />
}

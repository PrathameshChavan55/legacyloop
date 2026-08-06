import Placeholder from '../components/Placeholder'

/**
 * JobApplicants — owner: Member 3.
 *
 * Applicants for one job: shortlist, move status
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
export default function JobApplicants() {
  return <Placeholder title="JobApplicants" owner="Member 3" notes="Applicants for one job: shortlist, move status" />
}

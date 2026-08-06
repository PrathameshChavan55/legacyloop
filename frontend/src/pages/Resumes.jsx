import Placeholder from '../components/Placeholder'

/**
 * Resumes — owner: Member 4.
 *
 * Upload a PDF, list versions, set the default
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
export default function Resumes() {
  return <Placeholder title="Resumes" owner="Member 4" notes="Upload a PDF, list versions, set the default" />
}

import Placeholder from '../components/Placeholder'

/**
 * PostDetail — owner: Member 5.
 *
 * One post with its comment thread
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
export default function PostDetail() {
  return <Placeholder title="PostDetail" owner="Member 5" notes="One post with its comment thread" />
}

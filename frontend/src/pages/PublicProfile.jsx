import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, Clock, Edit3, MessageSquare, UserPlus } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Avatar, Badge, Button, Card, PageHeader, Spinner } from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'
import { useToast } from '../lib/toast'

/**
 * Public profile view for students and alumni.
 */
export default function PublicProfile() {
  const { userId } = useParams()
  const targetId = Number(userId)
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const isMe = user?.id === targetId

  const profile = useQuery({
    queryKey: ['profile', userId],
    queryFn: () => api.profiles.alumnus(targetId).catch(() => api.profiles.student(targetId)),
  })

  const posts = useQuery({
    queryKey: ['posts', 'author', userId],
    queryFn: () => api.posts.byAuthor(targetId, { size: 5 }),
  })

  const connectionsQuery = useQuery({
    queryKey: ['network', 'connections'],
    queryFn: () => api.network.connections({ size: 100 }),
    enabled: !isMe,
  })
  const sentQuery = useQuery({
    queryKey: ['network', 'sent'],
    queryFn: () => api.network.sent({ size: 100 }),
    enabled: !isMe,
  })
  const receivedQuery = useQuery({
    queryKey: ['network', 'received'],
    queryFn: () => api.network.received({ size: 100 }),
    enabled: !isMe,
  })

  const isConnected = connectionsQuery.data?.content?.some((c) => c.userId === targetId) ?? false
  const isSentPending = sentQuery.data?.content?.some((c) => c.userId === targetId) ?? false
  const receivedRequest = receivedQuery.data?.content?.find((c) => c.userId === targetId)

  const connect = useMutation({
    mutationFn: () => api.network.request(targetId, ''),
    onSuccess: () => {
      toast.success('Connection request sent!')
      queryClient.invalidateQueries({ queryKey: ['network'] })
    },
    onError: (err) => {
      toast.error(errorMessage(err))
    },
  })

  const accept = useMutation({
    mutationFn: () => api.network.accept(receivedRequest.id),
    onSuccess: () => {
      toast.success('Connection request accepted!')
      queryClient.invalidateQueries({ queryKey: ['network'] })
    },
    onError: (err) => {
      toast.error(errorMessage(err))
    },
  })

  const message = useMutation({
    mutationFn: () => api.chat.with(targetId),
    onSuccess: (conversation) => navigate(`/messages/${conversation.id}`),
    onError: (err) => {
      toast.error(errorMessage(err))
    },
  })

  if (profile.isLoading) return <Spinner />
  const person = profile.data
  if (!person) return <p className="text-sm text-slate-500">We could not find that profile.</p>

  return (
    <div className="max-w-3xl">
      <PageHeader title={person.fullName} subtitle={person.headline} />

      <Card className="flex flex-wrap items-center gap-4">
        <Avatar name={person.fullName} url={person.profilePhotoUrl} size={72} />

        <div className="flex-1 min-w-0">
          <p className="text-sm text-slate-600">
            {person.currentDesignation
              ? `${person.currentDesignation} at ${person.currentCompany}`
              : (person.batchName ?? 'Student')}
          </p>
          <p className="text-sm text-slate-500">
            {person.currentLocation ?? person.location ?? ''}
            {person.experienceLabel && ` · ${person.experienceLabel}`}
          </p>

          <div className="flex flex-wrap gap-1.5 mt-2">
            {isConnected && <Badge tone="green">Connected</Badge>}
            {person.willingToRefer && <Badge tone="green">Will refer</Badge>}
            {person.availableForMentorship && <Badge tone="brand">Mentors</Badge>}
            {person.openToWork && <Badge tone="brand">Open to work</Badge>}
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {isMe ? (
            <Link to="/profile">
              <Button variant="secondary" className="flex items-center gap-1.5">
                <Edit3 className="w-4 h-4" /> Edit Profile
              </Button>
            </Link>
          ) : (
            <>
              {isConnected ? (
                <Button
                  variant="primary"
                  onClick={() => message.mutate()}
                  loading={message.isPending}
                  className="flex items-center gap-1.5"
                >
                  <MessageSquare className="w-4 h-4" /> Message
                </Button>
              ) : isSentPending ? (
                <Button
                  variant="secondary"
                  disabled
                  className="bg-slate-50 text-slate-500 border-slate-200 cursor-default flex items-center gap-1.5"
                >
                  <Clock className="w-4 h-4 text-amber-500" /> Request Pending
                </Button>
              ) : receivedRequest ? (
                <Button
                  variant="primary"
                  onClick={() => accept.mutate()}
                  loading={accept.isPending}
                  className="flex items-center gap-1.5"
                >
                  <Check className="w-4 h-4" /> Accept Request
                </Button>
              ) : (
                <Button
                  onClick={() => connect.mutate()}
                  loading={connect.isPending}
                  className="flex items-center gap-1.5"
                >
                  <UserPlus className="w-4 h-4" /> Connect
                </Button>
              )}

            </>
          )}
        </div>
      </Card>

      {person.about && (
        <Card className="mt-4">
          <h2 className="text-base mb-2">About</h2>
          <p className="text-sm text-slate-700 whitespace-pre-wrap">{person.about}</p>
        </Card>
      )}

      {person.skills?.length > 0 && (
        <Card className="mt-4">
          <h2 className="text-base mb-3">Skills</h2>
          <div className="flex flex-wrap gap-2">
            {person.skills.map((skill) => (
              <Badge key={skill}>{skill}</Badge>
            ))}
          </div>
        </Card>
      )}

      {posts.data?.content?.length > 0 && (
        <Card className="mt-4">
          <h2 className="text-base mb-3">Recent posts</h2>
          <div className="space-y-3">
            {posts.data.content.map((post) => (
              <p key={post.id} className="text-sm text-slate-700 border-b border-slate-100 pb-3 last:border-0">
                {post.content}
              </p>
            ))}
          </div>
        </Card>
      )}
    </div>
  )
}

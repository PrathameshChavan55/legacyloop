import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, Clock, MessageSquare, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Avatar,
  Badge,
  Button,
  Card,
  EmptyState,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  Tabs,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'
import { useToast } from '../lib/toast'

/** Students, alumni and mentors directory with live connection tracking */
export default function Directory() {
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [tab, setTab] = useState('alumni')
  const [company, setCompany] = useState('')
  const [page, setPage] = useState(0)
  const [connectingId, setConnectingId] = useState(null)

  const companies = useQuery({ queryKey: ['alumni-companies'], queryFn: api.profiles.alumniCompanies })

  const people = useQuery({
    queryKey: ['directory', tab, company, page],
    queryFn: () =>
      tab === 'students'
        ? api.profiles.students({ page })
        : tab === 'mentors'
          ? api.profiles.mentors({ page })
          : api.profiles.alumni({ company, page }),
  })

  // Live network state for accurate button status
  const connectionsQuery = useQuery({
    queryKey: ['network', 'connections'],
    queryFn: () => api.network.connections({ size: 100 }),
  })
  const sentQuery = useQuery({
    queryKey: ['network', 'sent'],
    queryFn: () => api.network.sent({ size: 100 }),
  })
  const receivedQuery = useQuery({
    queryKey: ['network', 'received'],
    queryFn: () => api.network.received({ size: 100 }),
  })

  const connectedIds = new Set(connectionsQuery.data?.content?.map((c) => c.userId) ?? [])
  const sentPendingIds = new Set(sentQuery.data?.content?.map((c) => c.userId) ?? [])
  const receivedRequests = new Map(receivedQuery.data?.content?.map((c) => [c.userId, c.id]) ?? [])

  const connectMutation = useMutation({
    mutationFn: (targetUserId) => api.network.request(targetUserId, ''),
    onSuccess: (_, targetUserId) => {
      toast.success('Connection request sent!')
      queryClient.invalidateQueries({ queryKey: ['network'] })
    },
    onError: (err) => {
      toast.error(errorMessage(err))
    },
    onSettled: () => {
      setConnectingId(null)
    },
  })

  const acceptMutation = useMutation({
    mutationFn: (connectionId) => api.network.accept(connectionId),
    onSuccess: () => {
      toast.success('Connection request accepted!')
      queryClient.invalidateQueries({ queryKey: ['network'] })
    },
    onError: (err) => {
      toast.error(errorMessage(err))
    },
  })

  const [openingChatUserId, setOpeningChatUserId] = useState(null)
  const openChat = useMutation({
    mutationFn: (targetUserId) => api.chat.with(targetUserId),
    onSuccess: (conversation) => {
      navigate(`/messages/${conversation.id}`)
    },
    onError: (err) => {
      toast.error(errorMessage(err))
    },
    onSettled: () => {
      setOpeningChatUserId(null)
    },
  })

  // Filter out the logged-in user so they never see themselves
  const filteredPeople = people.data?.content?.filter((person) => person.userId !== user?.id) ?? []

  return (
    <>
      <PageHeader title="Directory" subtitle="Find people from your institution" />

      <Tabs
        active={tab}
        onChange={(value) => {
          setTab(value)
          setPage(0)
        }}
        tabs={[
          { value: 'alumni', label: 'Alumni' },
          { value: 'mentors', label: 'Mentors' },
          { value: 'students', label: 'Students' },
        ]}
      />

      {tab === 'alumni' && (
        <Card className="mb-5 max-w-xs">
          <Select
            value={company}
            onChange={(event) => {
              setCompany(event.target.value)
              setPage(0)
            }}
          >
            <option value="">Every company</option>
            {companies.data?.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </Select>
        </Card>
      )}

      {people.isLoading && <Spinner />}
      {!people.isLoading && filteredPeople.length === 0 && <EmptyState title="Nobody matches that" />}

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredPeople.map((person) => {
          const isConnected = connectedIds.has(person.userId)
          const isSentPending = sentPendingIds.has(person.userId)
          const receivedId = receivedRequests.get(person.userId)

          return (
            <Card key={person.userId} className="text-center flex flex-col justify-between hover:shadow-md transition-shadow">
              <div>
                <div className="flex justify-center">
                  <Avatar name={person.fullName} url={person.photoUrl} size={56} />
                </div>

                <Link to={`/people/${person.userId}`} className="block font-medium mt-3 hover:underline">
                  {person.fullName}
                </Link>
                <p className="text-sm text-slate-500 mt-0.5">{person.subtitle ?? person.headline ?? ''}</p>

                <div className="flex flex-wrap justify-center gap-1 mt-3">
                  {person.willingToRefer && <Badge tone="green">Will refer</Badge>}
                  {person.openToWork && <Badge tone="brand">Open to work</Badge>}
                  {person.skills?.slice(0, 3).map((skill) => (
                    <Badge key={skill}>{skill}</Badge>
                  ))}
                </div>
              </div>

              <div className="mt-4">
                {isConnected ? (
                  <Button
                    variant="secondary"
                    className="w-full text-xs flex items-center justify-center gap-1.5"
                    loading={openingChatUserId === person.userId}
                    onClick={() => {
                      setOpeningChatUserId(person.userId)
                      openChat.mutate(person.userId)
                    }}
                  >
                    <MessageSquare className="w-4 h-4 text-brand-600" />
                    Message
                  </Button>
                ) : isSentPending ? (
                  <Button
                    variant="secondary"
                    disabled
                    className="w-full text-xs text-slate-500 bg-slate-50 border-slate-200 cursor-default flex items-center justify-center gap-1.5"
                  >
                    <Clock className="w-4 h-4 text-amber-500" />
                    Request Sent
                  </Button>
                ) : receivedId ? (
                  <Button
                    variant="primary"
                    className="w-full text-xs flex items-center justify-center gap-1.5"
                    onClick={() => acceptMutation.mutate(receivedId)}
                    loading={acceptMutation.isPending}
                  >
                    <Check className="w-4 h-4" />
                    Accept Request
                  </Button>
                ) : (
                  <Button
                    variant="secondary"
                    className="w-full text-xs hover:border-brand-500 hover:text-brand-600 transition-colors flex items-center justify-center gap-1.5"
                    loading={connectingId === person.userId}
                    onClick={() => {
                      setConnectingId(person.userId)
                      connectMutation.mutate(person.userId)
                    }}
                  >
                    <UserPlus className="w-4 h-4" />
                    Connect
                  </Button>
                )}
              </div>
            </Card>
          )
        })}
      </div>

      <Pagination page={page} totalPages={people.data?.totalPages} onChange={setPage} />
    </>
  )
}

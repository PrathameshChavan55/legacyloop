import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Avatar,
  Button,
  Card,
  EmptyState,
  PageHeader,
  Spinner,
  Tabs,
} from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { timeAgo } from '../lib/format'
import { useToast } from '../lib/toast'

export default function Network() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const toast = useToast()
  const [tab, setTab] = useState('connections')
  const [openingChatUserId, setOpeningChatUserId] = useState(null)

  const summary = useQuery({ queryKey: ['network', 'summary'], queryFn: api.network.summary })
  const list = useQuery({
    queryKey: ['network', tab],
    queryFn: () =>
      tab === 'connections'
        ? api.network.connections({ size: 50 })
        : tab === 'received'
          ? api.network.received({ size: 50 })
          : api.network.sent({ size: 50 }),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['network'] })
  const act = useMutation({
    mutationFn: ({ id, action }) => api.network[action](id),
    onSuccess: invalidate,
    onError: (err) => toast.error(errorMessage(err)),
  })

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

  return (
    <>
      <PageHeader
        title="Network"
        subtitle={
          summary.data
            ? `${summary.data.connections} connections · ${summary.data.pendingReceived} waiting on you`
            : undefined
        }
      />

      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { value: 'connections', label: 'Connections' },
          { value: 'received', label: 'Requests' },
          { value: 'sent', label: 'Sent' },
        ]}
      />

      {list.isLoading && <Spinner />}
      {list.data?.content?.length === 0 && (
        <EmptyState
          title="Nobody here yet"
          message="Find people in the directory and send them a request."
          action={
            <Link to="/directory">
              <Button>Open the directory</Button>
            </Link>
          }
        />
      )}

      <div className="grid sm:grid-cols-2 gap-3">
        {list.data?.content?.map((connection) => (
          <Card key={connection.id} className="flex items-center gap-3">
            <Avatar name={connection.name} url={connection.photoUrl} />
            <div className="min-w-0 flex-1">
              <Link to={`/people/${connection.userId}`} className="font-medium text-sm hover:underline">
                {connection.name}
              </Link>
              <p className="text-xs text-slate-500 truncate">
                {connection.headline ?? timeAgo(connection.createdAt)}
              </p>
            </div>

            <div className="flex gap-1">
              {tab === 'received' && (
                <>
                  <Button onClick={() => act.mutate({ id: connection.id, action: 'accept' })}>Accept</Button>
                  <Button variant="ghost" onClick={() => act.mutate({ id: connection.id, action: 'reject' })}>
                    Ignore
                  </Button>
                </>
              )}
              {tab === 'sent' && (
                <Button variant="ghost" onClick={() => act.mutate({ id: connection.id, action: 'withdraw' })}>
                  Withdraw
                </Button>
              )}
              {tab === 'connections' && (
                <>
                  <Button
                    variant="secondary"
                    loading={openingChatUserId === connection.userId}
                    onClick={() => {
                      setOpeningChatUserId(connection.userId)
                      openChat.mutate(connection.userId)
                    }}
                  >
                    Message
                  </Button>
                  <Button variant="ghost" onClick={() => act.mutate({ id: connection.id, action: 'remove' })}>
                    Remove
                  </Button>
                </>
              )}
            </div>
          </Card>
        ))}
      </div>
    </>
  )
}

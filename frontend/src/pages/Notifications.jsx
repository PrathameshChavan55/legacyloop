import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Button,
  Card,
  EmptyState,
  PageHeader,
  Pagination,
  Spinner,
} from '../components/ui'
import { api } from '../lib/api'
import { cn, timeAgo } from '../lib/format'

export default function Notifications() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)

  const notifications = useQuery({
    queryKey: ['notifications', 'page', page],
    queryFn: () => api.notifications.inbox({ page }),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['notifications'] })
  const markAllRead = useMutation({ mutationFn: api.notifications.markAllRead, onSuccess: invalidate })
  const clearRead = useMutation({ mutationFn: api.notifications.clearRead, onSuccess: invalidate })
  const markRead = useMutation({ mutationFn: api.notifications.markRead, onSuccess: invalidate })

  if (notifications.isLoading) return <Spinner />

  return (
    <>
      <PageHeader
        title="Notifications"
        action={
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => markAllRead.mutate()}>
              Mark all read
            </Button>
            <Button variant="ghost" onClick={() => clearRead.mutate()}>
              Clear read
            </Button>
          </div>
        }
      />

      {notifications.data?.content?.length === 0 && (
        <EmptyState title="Nothing yet" message="Updates about your applications and network appear here." />
      )}

      <div className="space-y-2">
        {notifications.data?.content?.map((notification) => (
          <Card
            key={notification.id}
            className={cn('flex items-start justify-between gap-4', !notification.read && 'border-brand-200 bg-brand-50/40')}
          >
            <div>
              <p className="font-medium text-sm">{notification.title}</p>
              <p className="text-sm text-slate-600 mt-0.5">{notification.body}</p>
              <p className="text-xs text-slate-400 mt-1">{timeAgo(notification.createdAt)}</p>
            </div>

            <div className="flex gap-2 shrink-0">
              {notification.link && (
                <Link to={notification.link}>
                  <Button variant="secondary">Open</Button>
                </Link>
              )}
              {!notification.read && (
                <Button variant="ghost" onClick={() => markRead.mutate(notification.id)}>
                  Mark read
                </Button>
              )}
            </div>
          </Card>
        ))}
      </div>

      <Pagination page={page} totalPages={notifications.data?.totalPages} onChange={setPage} />
    </>
  )
}

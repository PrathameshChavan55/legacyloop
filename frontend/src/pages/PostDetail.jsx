import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Avatar, Button, Card, Spinner, Textarea } from '../components/ui'
import { api } from '../lib/api'
import { timeAgo } from '../lib/format'
import { PostCard } from './Feed'

export default function PostDetail() {
  const { postId } = useParams()
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState('')

  const post = useQuery({ queryKey: ['post', postId], queryFn: () => api.posts.byId(postId) })
  const comments = useQuery({
    queryKey: ['post', postId, 'comments'],
    queryFn: () => api.posts.comments(postId, { size: 30 }),
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['post', postId] })
    queryClient.invalidateQueries({ queryKey: ['posts'] })
  }

  const comment = useMutation({
    mutationFn: () => api.posts.comment(postId, draft),
    onSuccess: () => {
      setDraft('')
      invalidate()
    },
  })

  if (post.isLoading) return <Spinner />

  return (
    <div className="max-w-2xl">
      <Link to="/feed" className="text-sm link">
        ← Back to the feed
      </Link>

      <div className="mt-4 space-y-4">
        {post.data && <PostCard post={post.data} onChanged={invalidate} />}

        <Card>
          <h2 className="text-base mb-3">Comments</h2>

          <Textarea
            rows={2}
            placeholder="Add a comment"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
          />
          <div className="flex justify-end mt-2">
            <Button onClick={() => comment.mutate()} loading={comment.isPending} disabled={!draft.trim()}>
              Comment
            </Button>
          </div>

          <div className="mt-5 space-y-4">
            {comments.data?.content?.map((entry) => (
              <div key={entry.id} className="flex gap-3">
                <Avatar name={entry.author?.name} size={32} />
                <div>
                  <p className="text-sm">
                    <span className="font-medium">{entry.author?.name}</span>
                    <span className="text-xs text-slate-400 ml-2">{timeAgo(entry.createdAt)}</span>
                  </p>
                  <p className="text-sm text-slate-700 mt-0.5">{entry.content}</p>
                </div>
              </div>
            ))}

            {comments.data?.content?.length === 0 && (
              <p className="text-sm text-slate-500">No comments yet.</p>
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}

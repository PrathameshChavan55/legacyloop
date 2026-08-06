import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bookmark, MessageCircle, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Avatar,
  Button,
  Card,
  EmptyState,
  PageHeader,
  Pagination,
  Spinner,
  Tabs,
  Textarea,
} from '../components/ui'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { REACTIONS, cn, timeAgo } from '../lib/format'

export default function Feed() {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState('feed')
  const [page, setPage] = useState(0)
  const [draft, setDraft] = useState('')

  const posts = useQuery({
    queryKey: ['posts', tab, page],
    queryFn: () =>
      tab === 'feed'
        ? api.posts.feed({ page })
        : tab === 'saved'
          ? api.posts.saved({ page })
          : api.posts.search({ page }),
  })

  const trending = useQuery({ queryKey: ['hashtags'], queryFn: api.posts.trendingHashtags })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['posts'] })

  const create = useMutation({
    mutationFn: () => api.posts.create({ content: draft }),
    onSuccess: () => {
      setDraft('')
      invalidate()
    },
  })

  return (
    <div className="grid lg:grid-cols-[1fr_240px] gap-6">
      <div>
        <PageHeader title="Feed" subtitle="What your network is talking about" />

        <Card className="mb-5">
          <Textarea
            rows={3}
            placeholder="Share an update. Use #hashtags so people can find it."
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
          />
          <div className="flex justify-end mt-3">
            <Button onClick={() => create.mutate()} loading={create.isPending} disabled={!draft.trim()}>
              Post
            </Button>
          </div>
        </Card>

        <Tabs
          active={tab}
          onChange={(value) => {
            setTab(value)
            setPage(0)
          }}
          tabs={[
            { value: 'feed', label: 'Your network' },
            { value: 'explore', label: 'Explore' },
            { value: 'saved', label: 'Saved' },
          ]}
        />

        {posts.isLoading && <Spinner />}
        {posts.data?.content?.length === 0 && (
          <EmptyState
            title="Nothing here yet"
            message={
              tab === 'feed'
                ? 'Connect with people and their posts will show up here.'
                : 'Be the first to post something.'
            }
          />
        )}

        <div className="space-y-4">
          {posts.data?.content?.map((post) => (
            <PostCard key={post.id} post={post} onChanged={invalidate} />
          ))}
        </div>

        <Pagination page={page} totalPages={posts.data?.totalPages} onChange={setPage} />
      </div>

      <aside className="hidden lg:block">
        <Card>
          <h2 className="text-sm font-semibold mb-3">Trending</h2>
          {trending.data?.length ? (
            <ul className="space-y-2">
              {trending.data.map((tag) => (
                <li key={tag.tag}>
                  <Link to={`/feed?tag=${tag.tag}`} className="text-sm link">
                    #{tag.tag}
                  </Link>
                  <span className="text-xs text-slate-400 ml-2">{tag.count}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">No tags yet.</p>
          )}
        </Card>
      </aside>
    </div>
  )
}

/** Shared by the feed and the post detail page. */
export function PostCard({ post, onChanged }) {
  const { user } = useAuth()
  const react = useMutation({
    mutationFn: (type) => api.posts.react(post.id, type),
    onSuccess: onChanged,
  })
  const save = useMutation({ mutationFn: () => api.posts.toggleSave(post.id), onSuccess: onChanged })
  const remove = useMutation({
    mutationFn: () => api.posts.remove(post.id),
    onSuccess: onChanged,
  })

  const canDelete = user?.isAdmin || user?.id === post.author?.id

  return (
    <Card>
      <div className="flex items-start gap-3">
        <Avatar name={post.author?.name} url={post.author?.photoUrl} />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <Link to={`/people/${post.author?.id}`} className="font-medium text-sm hover:underline">
              {post.author?.name}
            </Link>
            <span className="text-xs text-slate-400">{timeAgo(post.createdAt)}</span>
            {post.edited && <span className="text-xs text-slate-400">· edited</span>}
          </div>

          <p className="text-sm text-slate-700 mt-2 whitespace-pre-wrap">{post.content}</p>

          {post.hashtags?.length > 0 && (
            <div className="flex flex-wrap gap-2 mt-2">
              {post.hashtags.map((tag) => (
                <span key={tag} className="text-xs text-brand-600">
                  #{tag}
                </span>
              ))}
            </div>
          )}

          <div className="flex items-center gap-1 mt-3 pt-3 border-t border-slate-100">
            {Object.entries(REACTIONS).map(([type, emoji]) => (
              <button
                key={type}
                onClick={() => react.mutate(type)}
                title={type.toLowerCase()}
                className={cn(
                  'px-2 py-1 rounded-lg text-sm hover:bg-slate-100',
                  post.myReaction === type && 'bg-brand-50 ring-1 ring-brand-200',
                )}
              >
                {emoji}
                <span className="ml-1 text-xs text-slate-500">
                  {post.reactionBreakdown?.[type] ?? 0}
                </span>
              </button>
            ))}

            <Link
              to={`/feed/${post.id}`}
              className="ml-auto flex items-center gap-1 text-xs text-slate-500 hover:text-slate-700 px-2 py-1"
            >
              <MessageCircle className="w-4 h-4" />
              {post.commentCount}
            </Link>

            <button
              onClick={() => save.mutate()}
              className={cn('p-1.5 rounded-lg hover:bg-slate-100', post.saved && 'text-brand-600')}
              aria-label="Save"
            >
              <Bookmark className="w-4 h-4" fill={post.saved ? 'currentColor' : 'none'} />
            </button>

            {canDelete && (
              <button
                onClick={() => {
                  if (window.confirm('Are you sure you want to delete this post?')) {
                    remove.mutate()
                  }
                }}
                className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50"
                aria-label="Delete post"
                title="Delete post"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    </Card>
  )
}

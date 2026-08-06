import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageSquare, MessageSquarePlus, Search, Send, Sparkles, User, Users } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Avatar, Badge, Button, Card, EmptyState, Input, PageHeader, Spinner } from '../components/ui'
import { api, errorMessage } from '../lib/api'
import { cn, timeAgo } from '../lib/format'
import { useRealtime } from '../lib/realtime'
import { useToast } from '../lib/toast'

/** The unified chat system: all connections and active conversations in one seamless view. */
export default function Messages() {
  const { conversationId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [draft, setDraft] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [openingUserId, setOpeningUserId] = useState(null)
  const messagesEndRef = useRef(null)

  // 1. Fetch existing conversation threads
  const conversations = useQuery({
    queryKey: ['conversations'],
    queryFn: () => api.chat.conversations(),
  })

  // 2. Fetch all user connections (e.g. Prathamesh, Anita, etc.)
  const connections = useQuery({
    queryKey: ['network', 'connections'],
    queryFn: () => api.network.connections({ size: 100 }),
  })

  // 3. Fetch active conversation details if conversationId is set
  const activeConversationDetails = useQuery({
    queryKey: ['conversation', conversationId],
    queryFn: () => api.chat.conversation(conversationId),
    enabled: Boolean(conversationId),
  })

  // 4. Fetch messages in active conversation
  const messages = useQuery({
    queryKey: ['messages', conversationId],
    queryFn: () => api.chat.messages(conversationId, { size: 50 }),
    enabled: Boolean(conversationId),
  })

  // Auto-scroll to bottom of chat
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages.data?.content, conversationId])

  // Real-time WebSocket listener for new incoming messages
  useRealtime(conversationId ? `/topic/conversations/${conversationId}` : null, () => {
    queryClient.invalidateQueries({ queryKey: ['messages', conversationId] })
    queryClient.invalidateQueries({ queryKey: ['conversations'] })
  })

  // Mark conversation read on mount / change
  useEffect(() => {
    if (conversationId) api.chat.markRead(conversationId).catch(() => {})
  }, [conversationId])

  // Send message mutation
  const send = useMutation({
    mutationFn: (contentToSend) =>
      api.chat.send({
        conversationId,
        content: (typeof contentToSend === 'string' ? contentToSend : draft).trim(),
      }),
    onSuccess: () => {
      setDraft('')
      queryClient.invalidateQueries({ queryKey: ['messages', conversationId] })
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
    },
    onError: (err) => {
      toast.error(errorMessage(err))
    },
  })

  // Start chat with user mutation
  const startChat = useMutation({
    mutationFn: (targetUserId) => api.chat.with(targetUserId),
    onSuccess: (conv) => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
      navigate(`/messages/${conv.id}`)
    },
    onError: (err) => {
      toast.error(errorMessage(err))
    },
    onSettled: () => {
      setOpeningUserId(null)
    },
  })

  const handleSelectContact = (item) => {
    if (item.conversationId) {
      navigate(`/messages/${item.conversationId}`)
    } else {
      setOpeningUserId(item.userId)
      startChat.mutate(item.userId)
    }
  }

  // Build unified contacts list (conversations + all network connections)
  const unifiedContacts = useMemo(() => {
    const convList = conversations.data?.content ?? []
    const connList = connections.data?.content ?? []

    // Map existing conversations by otherUserId
    const convByUserId = new Map()
    convList.forEach((conv) => {
      convByUserId.set(conv.otherUserId, conv)
    })

    // Map all connections
    const connByUserId = new Map()
    connList.forEach((conn) => {
      connByUserId.set(conn.userId, conn)
    })

    const items = []
    const processedUserIds = new Set()

    // 1. First add conversations that have active messages
    convList.forEach((conv) => {
      const conn = connByUserId.get(conv.otherUserId)
      items.push({
        conversationId: conv.id,
        userId: conv.otherUserId,
        name: conv.otherUserName || conn?.name || 'User',
        photoUrl: conv.otherUserPhotoUrl || conn?.photoUrl,
        headline: conn?.headline || '',
        lastMessage: conv.lastMessagePreview,
        lastMessageAt: conv.lastMessageAt,
        unread: conv.unread ?? 0,
        hasMessages: Boolean(conv.lastMessagePreview),
      })
      processedUserIds.add(conv.otherUserId)
    })

    // 2. Add connections that don't have a conversation yet (e.g. Prathamesh)
    connList.forEach((conn) => {
      if (!processedUserIds.has(conn.userId)) {
        items.push({
          conversationId: null,
          userId: conn.userId,
          name: conn.name,
          photoUrl: conn.photoUrl,
          headline: conn.headline || '',
          lastMessage: 'Connected · Click to message',
          lastMessageAt: null,
          unread: 0,
          hasMessages: false,
        })
        processedUserIds.add(conn.userId)
      }
    })

    // Filter by search term if provided
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase()
      return items.filter(
        (item) =>
          item.name.toLowerCase().includes(q) ||
          (item.lastMessage && item.lastMessage.toLowerCase().includes(q)) ||
          (item.headline && item.headline.toLowerCase().includes(q)),
      )
    }

    return items
  }, [conversations.data, connections.data, searchQuery])

  // Identify active chat partner details
  const activePartner = useMemo(() => {
    if (!conversationId) return null

    // Check active conversation query details
    if (activeConversationDetails.data) {
      return {
        userId: activeConversationDetails.data.otherUserId,
        name: activeConversationDetails.data.otherUserName,
        photoUrl: activeConversationDetails.data.otherUserPhotoUrl,
      }
    }

    // Check conversations list
    const inConv = conversations.data?.content?.find((c) => c.id === conversationId)
    if (inConv) {
      return {
        userId: inConv.otherUserId,
        name: inConv.otherUserName,
        photoUrl: inConv.otherUserPhotoUrl,
      }
    }

    // Check contacts list
    const inUnified = unifiedContacts.find((c) => c.conversationId === conversationId)
    if (inUnified) {
      return {
        userId: inUnified.userId,
        name: inUnified.name,
        photoUrl: inUnified.photoUrl,
      }
    }

    return null
  }, [conversationId, activeConversationDetails.data, conversations.data, unifiedContacts])

  const thread = useMemo(() => [...(messages.data?.content ?? [])].reverse(), [messages.data?.content])

  return (
    <>
      <PageHeader
        title="Messages"
        subtitle="Chat directly with your alumni, mentors, and peers"
      />

      <div className="grid md:grid-cols-[320px_1fr] gap-4 h-[75vh]">
        {/* Left Column: Unified Contacts & Inbox */}
        <Card className="p-0 flex flex-col h-full overflow-hidden border-slate-200">
          {/* Search Header */}
          <div className="p-3 border-b border-slate-100 bg-slate-50/50">
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
              <input
                type="text"
                placeholder="Search chats or connections..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 transition"
              />
            </div>
            <div className="flex items-center justify-between mt-2 px-1 text-[11px] font-medium text-slate-500">
              <span>ALL CONTACTS ({unifiedContacts.length})</span>
              {connections.data?.content?.length > 0 && (
                <span className="text-emerald-600 font-semibold flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block"></span>
                  {connections.data.content.length} Connected
                </span>
              )}
            </div>
          </div>

          {/* Contacts List */}
          <div className="flex-1 overflow-y-auto divide-y divide-slate-100">
            {(conversations.isLoading || connections.isLoading) && (
              <div className="p-8 flex justify-center">
                <Spinner />
              </div>
            )}

            {!conversations.isLoading && !connections.isLoading && unifiedContacts.length === 0 && (
              <div className="p-8 text-center">
                <Users className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                <p className="text-sm font-medium text-slate-700">No connections yet</p>
                <p className="text-xs text-slate-500 mt-1 mb-4">
                  Connect with students & alumni from the directory to start messaging.
                </p>
                <Link to="/directory">
                  <Button variant="secondary" className="w-full text-xs">
                    Browse Directory
                  </Button>
                </Link>
              </div>
            )}

            {unifiedContacts.map((contact) => {
              const isSelected = contact.conversationId && contact.conversationId === conversationId
              const isOpening = openingUserId === contact.userId

              return (
                <button
                  key={contact.userId}
                  onClick={() => handleSelectContact(contact)}
                  disabled={isOpening}
                  className={cn(
                    'w-full text-left px-3.5 py-3 flex gap-3 items-center hover:bg-slate-50 transition-colors relative group',
                    isSelected && 'bg-brand-50/90 border-l-4 border-l-brand-600',
                  )}
                >
                  <div className="relative">
                    <Avatar name={contact.name} url={contact.photoUrl} size={42} />
                    <span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-white" />
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between">
                      <p
                        className={cn(
                          'text-sm font-medium truncate',
                          isSelected ? 'text-brand-900 font-semibold' : 'text-slate-900',
                        )}
                      >
                        {contact.name}
                      </p>
                      {contact.lastMessageAt && (
                        <span className="text-[10px] text-slate-400 shrink-0 ml-1">
                          {timeAgo(contact.lastMessageAt)}
                        </span>
                      )}
                    </div>

                    <p
                      className={cn(
                        'text-xs truncate mt-0.5',
                        contact.hasMessages ? 'text-slate-500' : 'text-brand-600 font-medium',
                      )}
                    >
                      {contact.lastMessage}
                    </p>
                  </div>

                  {contact.unread > 0 && (
                    <span className="min-w-5 h-5 px-1.5 rounded-full bg-brand-600 text-white text-[11px] grid place-items-center font-bold shadow-sm">
                      {contact.unread}
                    </span>
                  )}

                  {isOpening && <Spinner />}
                </button>
              )
            })}
          </div>
        </Card>

        {/* Right Column: Active Conversation / Empty State */}
        <Card className="p-0 flex flex-col h-full overflow-hidden border-slate-200">
          {!conversationId ? (
            <div className="flex-1 flex flex-col items-center justify-center p-8 text-center bg-slate-50/30">
              <div className="w-16 h-16 rounded-2xl bg-brand-50 flex items-center justify-center text-brand-600 mb-4 shadow-sm border border-brand-100">
                <MessageSquare className="w-8 h-8" />
              </div>
              <h3 className="text-lg font-semibold text-slate-800">Your Messages</h3>
              <p className="text-sm text-slate-500 max-w-md mt-1 mb-6">
                Choose a connection from the list on the left, or click on one of your connections below to start chatting.
              </p>

              {connections.data?.content?.length > 0 && (
                <div className="w-full max-w-lg bg-white rounded-xl p-5 border border-slate-200 shadow-xs">
                  <div className="flex items-center justify-between mb-3">
                    <p className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Connected Network ({connections.data.content.length})
                    </p>
                    <span className="text-[11px] text-slate-400">Click to start conversation</span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    {connections.data.content.map((conn) => (
                      <button
                        key={conn.id}
                        onClick={() => {
                          setOpeningUserId(conn.userId)
                          startChat.mutate(conn.userId)
                        }}
                        disabled={openingUserId === conn.userId}
                        className="flex items-center gap-3 p-2.5 bg-slate-50 hover:bg-brand-50/70 border border-slate-100 hover:border-brand-200 rounded-lg transition text-left group"
                      >
                        <div className="relative">
                          <Avatar name={conn.name} url={conn.photoUrl} size={36} />
                          <span className="absolute bottom-0 right-0 w-2 h-2 rounded-full bg-emerald-500 ring-1 ring-white" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-semibold text-slate-900 group-hover:text-brand-700 truncate">
                            {conn.name}
                          </p>
                          <p className="text-[11px] text-slate-500 truncate">
                            {conn.headline || 'Connected'}
                          </p>
                        </div>
                        {openingUserId === conn.userId ? (
                          <Spinner />
                        ) : (
                          <MessageSquarePlus className="w-4 h-4 text-slate-400 group-hover:text-brand-600 transition shrink-0" />
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <>
              {/* Active Header */}
              <div className="px-5 py-3.5 border-b border-slate-200 flex items-center justify-between bg-white shadow-xs">
                <div className="flex items-center gap-3">
                  <div className="relative">
                    <Avatar
                      name={activePartner?.name ?? 'User'}
                      url={activePartner?.photoUrl}
                      size={40}
                    />
                    <span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-white" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 leading-tight">
                      {activePartner?.name ?? 'Chat'}
                    </h3>
                    <p className="text-[11px] text-emerald-600 font-medium flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block"></span>
                      Connected · Online
                    </p>
                  </div>
                </div>

                {activePartner?.userId && (
                  <Link
                    to={`/people/${activePartner.userId}`}
                    className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1.5 bg-brand-50 hover:bg-brand-100/70 px-3 py-1.5 rounded-lg border border-brand-100 transition"
                  >
                    <User className="w-3.5 h-3.5" />
                    View Profile
                  </Link>
                )}
              </div>

              {/* Messages Thread Feed */}
              <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50/50">
                {messages.isLoading && (
                  <div className="py-12 flex justify-center">
                    <Spinner />
                  </div>
                )}

                {!messages.isLoading && thread.length === 0 && (
                  <div className="flex flex-col items-center justify-center py-10 text-center">
                    <div className="w-12 h-12 rounded-full bg-brand-50 text-brand-600 grid place-items-center mb-3">
                      <Sparkles className="w-6 h-6" />
                    </div>
                    <p className="text-sm font-semibold text-slate-800">
                      Say hello to {activePartner?.name ?? 'your connection'}!
                    </p>
                    <p className="text-xs text-slate-500 max-w-xs mt-1 mb-4">
                      You are connected. Send a message to start exchanging ideas, referrals, or advice.
                    </p>

                    {/* Quick message starters */}
                    <div className="flex flex-wrap justify-center gap-2 max-w-md">
                      {[
                        `👋 Hi ${activePartner?.name?.split(' ')[0] ?? ''}, glad to connect with you!`,
                        'Hey! How are things going?',
                        'Would love to learn more about your experience.',
                      ].map((starter) => (
                        <button
                          key={starter}
                          onClick={() => setDraft(starter)}
                          className="text-xs px-3 py-1.5 bg-white border border-slate-200 hover:border-brand-300 hover:bg-brand-50/50 rounded-full text-slate-700 transition"
                        >
                          {starter}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {thread.map((message) => (
                  <div key={message.id} className={cn('flex', message.mine && 'justify-end')}>
                    <div
                      className={cn(
                        'max-w-[75%] rounded-2xl px-4 py-2.5 shadow-xs',
                        message.mine
                          ? 'bg-brand-600 text-white rounded-br-xs'
                          : 'bg-white border border-slate-200 text-slate-800 rounded-bl-xs',
                      )}
                    >
                      <p className="text-sm whitespace-pre-wrap leading-relaxed">{message.content}</p>
                      <p
                        className={cn(
                          'text-[10px] mt-1 text-right',
                          message.mine ? 'text-brand-200' : 'text-slate-400',
                        )}
                      >
                        {timeAgo(message.createdAt)}
                      </p>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>

              {/* Input Form */}
              <form
                className="border-t border-slate-200 p-3 flex gap-2 bg-white"
                onSubmit={(event) => {
                  event.preventDefault()
                  if (draft.trim() && !send.isPending) send.mutate(draft)
                }}
              >
                <Input
                  placeholder={
                    activePartner?.name
                      ? `Message ${activePartner.name}...`
                      : 'Write a message...'
                  }
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  className="flex-1"
                  autoFocus
                />
                <button
                  type="submit"
                  className="rounded-lg bg-brand-600 hover:bg-brand-700 active:bg-brand-800 text-white px-4 flex items-center justify-center transition disabled:opacity-50 disabled:cursor-not-allowed shadow-xs"
                  disabled={!draft.trim() || send.isPending}
                >
                  <Send className="w-4 h-4" />
                </button>
              </form>
            </>
          )}
        </Card>
      </div>
    </>
  )
}

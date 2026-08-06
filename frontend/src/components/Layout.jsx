import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Bell,
  Briefcase,
  Building2,
  CreditCard,
  FileText,
  GraduationCap,
  Home,
  LogOut,
  MessageSquare,
  Newspaper,
  Send,
  Shield,
  Sparkles,
  User,
  Users,
  X,
  Lock,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { cn, roleLabel, timeAgo } from '../lib/format'
import { useRealtime } from '../lib/realtime'
import { Avatar, Button, EmptyState } from './ui'

/**
 * The shell every signed-in page renders inside: a sidebar, a top bar and the notification panel.
 *
 * The original had AppLayout, AuthLayout, Sidebar, Topbar and NotificationPanel as five files that
 * only ever appeared together. The navigation is a list filtered by role, so adding a screen is a
 * line rather than a new component.
 */

const NAV = [
  { to: '/', label: 'Home', icon: Home, roles: null },
  { to: '/feed', label: 'Feed', icon: Newspaper, roles: null },
  { to: '/jobs', label: 'Jobs', icon: Briefcase, roles: null },
  { to: '/applications', label: 'Applications', icon: FileText, roles: ['STUDENT'] },
  { to: '/referrals', label: 'Referrals', icon: Send, roles: ['STUDENT', 'ALUMNI'] },
  { to: '/resumes', label: 'Resumes', icon: FileText, roles: ['STUDENT', 'ALUMNI'] },
  { to: '/ai', label: 'AI tools', icon: Sparkles, roles: ['STUDENT', 'ALUMNI'], premium: true },
  { to: '/network', label: 'Network', icon: Users, roles: null },
  { to: '/directory', label: 'Directory', icon: GraduationCap, roles: null },
  { to: '/messages', label: 'Messages', icon: MessageSquare, roles: null },
  { to: '/jobs/manage', label: 'Manage jobs', icon: Building2, roles: ['INSTITUTION_STAFF', 'ALUMNI', 'PLATFORM_ADMIN'] },
  { to: '/academics', label: 'Academics', icon: GraduationCap, roles: ['INSTITUTION_STAFF', 'PLATFORM_ADMIN'] },
  { to: '/admin', label: 'Administration', icon: Shield, roles: ['PLATFORM_ADMIN'] },
  { to: '/premium', label: 'Premium', icon: CreditCard, roles: null },
]

export default function Layout() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [panelOpen, setPanelOpen] = useState(false)
  const panelRef = useRef(null)
  const buttonRef = useRef(null)

  useEffect(() => {
    function handleClickOutside(event) {
      if (
        panelOpen &&
        panelRef.current &&
        !panelRef.current.contains(event.target) &&
        buttonRef.current &&
        !buttonRef.current.contains(event.target)
      ) {
        setPanelOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [panelOpen])

  const unread = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: api.notifications.unreadCount,
    refetchInterval: 60_000,
  })

  // The bell updates as events arrive rather than only on the next poll.
  useRealtime(user ? `/topic/notifications/${user.id}` : null, () =>
    queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  )

  const visible = NAV.filter((item) => !item.roles || hasRole(...item.roles))

  const signOut = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex">
      <aside className="hidden md:flex w-60 flex-col border-r border-slate-200 bg-white">
        <Link to="/" className="flex items-center gap-2 px-5 h-16 border-b border-slate-200">
          <span className="w-8 h-8 rounded-lg bg-brand-600 text-white grid place-items-center font-bold">
            L
          </span>
          <span className="font-semibold text-slate-900">LegacyLoop</span>
        </Link>

        <nav className="flex-1 overflow-y-auto py-3">
          {visible.map(({ to, label, icon: Icon, premium }) => {
            const isLocked = premium && !user?.premium
            return (
              <NavLink
                key={to}
                to={isLocked ? '/premium' : to}
                end={to === '/'}
                className={({ isActive }) =>
                  cn(
                    'flex items-center justify-between px-5 py-2.5 text-sm w-full',
                    isActive && !isLocked
                      ? 'text-brand-700 bg-brand-50 font-medium border-r-2 border-brand-600'
                      : 'text-slate-600 hover:bg-slate-50',
                  )
                }
              >
                <div className="flex items-center gap-3">
                  <Icon className="w-4 h-4" />
                  {label}
                </div>
                {isLocked && <Lock className="w-3.5 h-3.5 text-slate-400" />}
              </NavLink>
            )
          })}
        </nav>

        <button
          onClick={signOut}
          className="flex items-center gap-3 px-5 py-3 text-sm text-slate-600 hover:bg-slate-50 border-t border-slate-200"
        >
          <LogOut className="w-4 h-4" />
          Sign out
        </button>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 border-b border-slate-200 bg-white flex items-center justify-end gap-3 px-5">
          <button
            ref={buttonRef}
            onClick={() => setPanelOpen((open) => !open)}
            className="relative p-2 rounded-lg text-slate-500 hover:bg-slate-100"
            aria-label="Notifications"
          >
            <Bell className="w-5 h-5" />
            {unread.data?.count > 0 && (
              <span className="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-full bg-red-500 text-white text-[10px] grid place-items-center">
                {unread.data.count > 9 ? '9+' : unread.data.count}
              </span>
            )}
          </button>

          <Link to="/profile" className="flex items-center gap-2">
            <Avatar name={user?.fullName} size={32} />
            <span className="hidden sm:block text-sm">
              <span className="block font-medium text-slate-800 leading-tight">{user?.fullName}</span>
              <span className="block text-xs text-slate-500">{roleLabel(user?.roles?.[0])}</span>
            </span>
          </Link>
        </header>

        {panelOpen && <NotificationPanel innerRef={panelRef} onClose={() => setPanelOpen(false)} />}

        <main className="flex-1 p-5 md:p-8 max-w-6xl w-full mx-auto">
          <Outlet />
        </main>

        {/* Mobile navigation: the same list, trimmed to what fits. */}
        <nav className="md:hidden border-t border-slate-200 bg-white flex justify-around py-2">
          {visible.slice(0, 5).map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                cn('flex flex-col items-center text-[10px] gap-1 px-2',
                  isActive ? 'text-brand-700' : 'text-slate-500')
              }
            >
              <Icon className="w-5 h-5" />
              {label}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  )
}

function NotificationPanel({ onClose, innerRef }) {
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['notifications', 'panel'],
    queryFn: () => api.notifications.inbox({ size: 8 }),
  })

  const markAllRead = async () => {
    await api.notifications.markAllRead()
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
  }

  const clearRead = async () => {
    await api.notifications.clearRead()
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
  }

  const deleteOne = async (e, id) => {
    e.preventDefault()
    e.stopPropagation()
    await api.notifications.delete(id)
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
  }

  return (
    <div ref={innerRef} className="absolute right-5 top-16 z-40 w-80 card overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
        <span className="font-medium text-sm">Notifications</span>
        <div className="flex items-center gap-1.5">
          <button className="text-xs link" onClick={markAllRead}>
            Mark all read
          </button>
          <span className="text-slate-300 text-xs">|</span>
          <button className="text-xs link text-slate-500 hover:text-brand-600" onClick={clearRead}>
            Clear read
          </button>
        </div>
      </div>

      <div className="max-h-96 overflow-y-auto divide-y divide-slate-100">
        {isLoading && <p className="p-4 text-sm text-slate-500">Loading…</p>}
        {data?.content?.length === 0 && (
          <EmptyState title="Nothing yet" message="Updates about your applications will appear here." />
        )}
        {data?.content?.map((notification) => (
          <div key={notification.id} className="relative group">
            <Link
              to={notification.link ?? '/notifications'}
              onClick={onClose}
              className={cn('block px-4 py-3 pr-10 hover:bg-slate-50', !notification.read && 'bg-brand-50/50')}
            >
              <p className="text-sm font-medium text-slate-800">{notification.title}</p>
              <p className="text-xs text-slate-600 mt-0.5">{notification.body}</p>
              <p className="text-[11px] text-slate-400 mt-1">{timeAgo(notification.createdAt)}</p>
            </Link>
            <button
              onClick={(e) => deleteOne(e, notification.id)}
              className="absolute top-3 right-3 p-1 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 opacity-0 group-hover:opacity-100 transition-opacity"
              title="Delete notification"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        ))}
      </div>

      <div className="p-2 border-t border-slate-200">
        <Link to="/notifications" onClick={onClose}>
          <Button variant="ghost" className="w-full">
            See all
          </Button>
        </Link>
      </div>
    </div>
  )
}

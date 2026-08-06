/** Small shared helpers. Nothing here is worth a module of its own. */

export const cn = (...classes) => classes.filter(Boolean).join(' ')

export const ROLES = {
  STUDENT: 'Student',
  ALUMNI: 'Alumnus',
  INSTITUTION_STAFF: 'Placement staff',
  PLATFORM_ADMIN: 'Administrator',
}

export const roleLabel = (role) => ROLES[String(role).replace('ROLE_', '')] ?? role

export const APPLICATION_STATUSES = [
  'APPLIED',
  'UNDER_REVIEW',
  'SHORTLISTED',
  'REFERRED',
  'INTERVIEW_SCHEDULED',
  'SELECTED',
  'REJECTED',
  'WITHDRAWN',
]

export const JOB_TYPES = ['FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'CONTRACT']
export const WORK_MODES = ['ONSITE', 'REMOTE', 'HYBRID']
export const REACTIONS = { LIKE: '👍', CELEBRATE: '🎉', SUPPORT: '🤝', INSIGHTFUL: '💡' }

/** UNDER_REVIEW becomes "Under review". */
export const titleCase = (value) =>
  value ? value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ') : ''

export const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : ''

export const formatDateTime = (value) =>
  value
    ? new Date(value).toLocaleString('en-IN', {
        day: 'numeric',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
      })
    : ''

/** "3 hours ago" — falls back to a date once something is more than a week old. */
export const timeAgo = (value) => {
  if (!value) return ''
  const seconds = Math.floor((Date.now() - new Date(value).getTime()) / 1000)
  if (seconds < 60) return 'just now'

  const units = [
    [86400, 'day'],
    [3600, 'hr'],
    [60, 'min'],
  ]
  if (seconds >= 86400 * 7) return formatDate(value)

  for (const [size, label] of units) {
    const count = Math.floor(seconds / size)
    if (count >= 1) return `${count} ${label}${count === 1 ? '' : 's'} ago`
  }
  return 'just now'
}

export const rupees = (paise) =>
  paise == null ? '' : `₹${(paise / 100).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`

export const initials = (name) =>
  (name ?? '?')
    .split(' ')
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')

import axios from 'axios'

/**
 * The whole HTTP layer: one axios instance, one refresh rule, and every endpoint the app calls.
 *
 * The original split this across four api files plus an apiClient plus an apiError module. There
 * is one backend contract, so there is one file describing it — and grouping the calls by feature
 * below keeps it readable without spreading it over the filesystem.
 */

const TOKEN_KEY = 'legacyloop.tokens'

export const tokens = {
  read: () => {
    try {
      return JSON.parse(localStorage.getItem(TOKEN_KEY)) ?? null
    } catch {
      return null
    }
  },
  write: (value) => localStorage.setItem(TOKEN_KEY, JSON.stringify(value)),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

const http = axios.create({ baseURL: '/api/v1', headers: { 'Content-Type': 'application/json' } })

http.interceptors.request.use((config) => {
  const stored = tokens.read()
  if (stored?.accessToken) config.headers.Authorization = `Bearer ${stored.accessToken}`
  return config
})

/**
 * On a 401 the access token is swapped for a fresh one and the original request is retried, once.
 * `retried` is what stops a failing refresh from looping.
 */
let refreshing = null
http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config
    const stored = tokens.read()

    if (error.response?.status === 401 && stored?.refreshToken && !request._retried) {
      request._retried = true
      try {
        refreshing =
          refreshing ??
          axios
            .post('/api/v1/auth/refresh', { refreshToken: stored.refreshToken })
            .then((response) => response.data.data)
            .finally(() => {
              refreshing = null
            })

        const fresh = await refreshing
        tokens.write(fresh)
        request.headers.Authorization = `Bearer ${fresh.accessToken}`
        return http(request)
      } catch {
        tokens.clear()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

/** Every response is `{success, data, error}`; callers only ever want `data`. */
const unwrap = (response) => response.data?.data

/** Turns any axios failure into the one sentence worth showing a person. */
export const errorMessage = (error) =>
  error?.response?.data?.error?.fieldErrors?.[0]?.message ??
  error?.response?.data?.message ??
  error?.message ??
  'Something went wrong'

const get = (url, params) => http.get(url, { params }).then(unwrap)
const post = (url, body) => http.post(url, body).then(unwrap)
const put = (url, body) => http.put(url, body).then(unwrap)
const patch = (url, body) => http.patch(url, body).then(unwrap)
const remove = (url, body) => http.delete(url, { data: body }).then(unwrap)

export const api = {
  auth: {
    register: (body) => post('/auth/register', body),
    verifyOtp: (body) => post('/auth/verify-otp', body),
    resendOtp: (email) => post('/auth/resend-otp', { email }),
    login: (body) => post('/auth/login', body),
    logout: (refreshToken) => post('/auth/logout', { refreshToken }),
    forgotPassword: (email) => post('/auth/forgot-password', { email }),
    resetPassword: (body) => post('/auth/reset-password', body),
    changePassword: (body) => post('/auth/change-password', body),
    me: () => get('/auth/me'),
  },

  institutions: {
    branding: () => get('/institutions/branding'),
    search: (params) => get('/institutions', params),
    byId: (id) => get(`/institutions/${id}`),
    create: (body) => post('/institutions', body),
    update: (id, body) => put(`/institutions/${id}`, body),
    deactivate: (id) => patch(`/institutions/${id}/deactivate`),
    reactivate: (id) => patch(`/institutions/${id}/reactivate`),
  },

  /** One call shape for departments, programs, branches and batches. */
  academics: {
    list: (type, params) => get(`/academics/${type}`, params),
    placementOpenBatches: () => get('/academics/batches/placement-open'),
    create: (type, body) => post(`/academics/${type}`, body),
    update: (type, id, body) => put(`/academics/${type}/${id}`, body),
    deactivate: (type, id) => patch(`/academics/${type}/${id}/deactivate`),
    reactivate: (type, id) => patch(`/academics/${type}/${id}/reactivate`),
  },

  profiles: {
    myStudent: () => get('/profiles/me/student'),
    saveStudent: (body) => put('/profiles/me/student', body),
    myAlumni: () => get('/profiles/me/alumni'),
    saveAlumni: (body) => put('/profiles/me/alumni', body),
    student: (userId) => get(`/profiles/students/${userId}`),
    alumnus: (userId) => get(`/profiles/alumni/${userId}`),
    students: (params) => get('/profiles/students', params),
    alumni: (params) => get('/profiles/alumni', params),
    mentors: (params) => get('/profiles/mentors', params),
    alumniCompanies: () => get('/profiles/alumni/companies'),
    suggestSkills: (query) => get('/profiles/skills/suggest', { query }),
    popularSkills: () => get('/profiles/skills/popular'),
    uploadPhoto: (file) => {
      const form = new FormData()
      form.append('file', file)
      return http
        .post('/profiles/me/photo', form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        .then(unwrap)
    },
  },

  admin: {
    users: (params) => get('/admin/users', params),
    user: (id) => get(`/admin/users/${id}`),
    statistics: () => get('/admin/users/statistics'),
    auditLogs: (params) => get('/admin/users/audit-logs', params),
    userAuditLogs: (id, params) => get(`/admin/users/${id}/audit-logs`, params),
    loginHistory: (id, params) => get(`/admin/users/${id}/login-history`, params),
    create: (body) => post('/admin/users', body),
    approve: (id) => patch(`/admin/users/${id}/approve`),
    verify: (id) => patch(`/admin/users/${id}/verify`),
    suspend: (id, reason) => patch(`/admin/users/${id}/suspend`, { reason }),
    reactivate: (id) => patch(`/admin/users/${id}/reactivate`),
    deleteUser: (id) => remove(`/admin/users/${id}`),
    forcePasswordReset: (id) => post(`/admin/users/${id}/force-password-reset`),
  },

  jobs: {
    search: (params) => get('/jobs/search', params),
    manage: (params) => get('/jobs/manage', params),
    mine: (params) => get('/jobs/mine', params),
    byId: (id) => get(`/jobs/${id}`),
    create: (body) => post('/jobs', body),
    update: (id, body) => put(`/jobs/${id}`, body),
    publish: (id) => patch(`/jobs/${id}/publish`),
    close: (id) => patch(`/jobs/${id}/close`),
    reopen: (id) => patch(`/jobs/${id}/reopen`),
    companies: (params) => get('/jobs/companies', params),
    activeCompanies: () => get('/jobs/companies/active'),
    createCompany: (body) => post('/jobs/companies', body),
    updateCompany: (id, body) => put(`/jobs/companies/${id}`, body),
    verifyCompany: (id) => patch(`/jobs/companies/${id}/verify`),
    deactivateCompany: (id) => patch(`/jobs/companies/${id}/deactivate`),
  },

  applications: {
    apply: (jobId, body) => post(`/applications/jobs/${jobId}`, body),
    mine: (params) => get('/applications/me', params),
    mineById: (id) => get(`/applications/me/${id}`),
    withdraw: (id) => patch(`/applications/me/${id}/withdraw`),
    forJob: (jobId, params) => get(`/applications/jobs/${jobId}`, params),
    review: (id) => get(`/applications/${id}/review`),
    changeStatus: (id, body) => patch(`/applications/${id}/status`, body),
    referrers: (id) => get(`/applications/${id}/referrers`),
    requestReferrals: (id, body) => post(`/applications/${id}/referrals`, body),
    referralsReceived: (params) => get('/applications/referrals/received', params),
    referralsSent: (params) => get('/applications/referrals/sent', params),
    respondToReferral: (id, body) => patch(`/applications/referrals/${id}/respond`, body),
    withdrawReferral: (id) => patch(`/applications/referrals/${id}/withdraw`),
  },

  resumes: {
    list: () => get('/resumes'),
    upload: (file, label) => {
      const form = new FormData()
      form.append('file', file)
      return http
        .post('/resumes', form, {
          params: label ? { label } : undefined,
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        .then(unwrap)
    },
    downloadUrl: (id) => {
      const stored = tokens.read()
      const token = stored?.accessToken
      return `/api/v1/resumes/${id}/download${token ? `?token=${encodeURIComponent(token)}` : ''}`
    },
    viewResume: async (id) => {
      try {
        const response = await http.get(`/resumes/${id}/download`, { responseType: 'blob' })
        const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/pdf' })
        const url = window.URL.createObjectURL(blob)
        window.open(url, '_blank')
      } catch (err) {
        const stored = tokens.read()
        const token = stored?.accessToken
        window.open(`/api/v1/resumes/${id}/download${token ? `?token=${encodeURIComponent(token)}` : ''}`, '_blank')
      }
    },
    makePrimary: (id) => patch(`/resumes/${id}/primary`),
    rename: (id, label) => patch(`/resumes/${id}/rename`, { label }),
    remove: (id) => remove(`/resumes/${id}`),
  },

  ai: {
    analyse: (resumeId, jobId) => post(`/ai/resumes/${resumeId}/analyze`, { jobId }),
    analysis: (id) => get(`/ai/analyses/${id}`),
    latestForResume: (resumeId) => get(`/ai/resumes/${resumeId}/analysis`),
    history: (params) => get('/ai/analyses', params),
    buildResume: (body) => post('/ai/resume-builder', body),
    interviewQuestions: (body) => post('/ai/interview/questions', body),
    interviewFeedback: (body) => post('/ai/interview/feedback', body),
  },

  analytics: {
    dashboard: () => get('/analytics/dashboard'),
    refresh: () => post('/analytics/dashboard/refresh'),
    exportUrl: (report) => `/api/v1/analytics/export/${report}`,
  },

  posts: {
    create: (body) => post('/posts', body),
    feed: (params) => get('/posts/feed', params),
    search: (params) => get('/posts/search', params),
    saved: (params) => get('/posts/saved', params),
    byId: (id) => get(`/posts/${id}`),
    byAuthor: (authorId, params) => get(`/posts/author/${authorId}`, params),
    byHashtag: (tag, params) => get(`/posts/hashtag/${tag}`, params),
    update: (id, body) => put(`/posts/${id}`, body),
    remove: (id) => remove(`/posts/${id}`),
    comments: (id, params) => get(`/posts/${id}/comments`, params),
    comment: (id, content) => post(`/posts/${id}/comments`, { content }),
    replies: (commentId, params) => get(`/posts/comments/${commentId}/replies`, params),
    reply: (commentId, content) => post(`/posts/comments/${commentId}/replies`, { content }),
    deleteComment: (commentId) => remove(`/posts/comments/${commentId}`),
    react: (id, type) => post(`/posts/${id}/reactions`, { type }),
    toggleSave: (id) => post(`/posts/${id}/save`),
    trendingHashtags: () => get('/posts/hashtags/trending'),
  },

  network: {
    request: (userId, message) => post('/network/requests', { userId, message }),
    received: (params) => get('/network/requests/received', params),
    sent: (params) => get('/network/requests/sent', params),
    accept: (id) => patch(`/network/requests/${id}/accept`),
    reject: (id) => patch(`/network/requests/${id}/reject`),
    withdraw: (id) => patch(`/network/requests/${id}/withdraw`),
    connections: (params) => get('/network/connections', params),
    remove: (id) => remove(`/network/connections/${id}`),
    summary: () => get('/network/summary'),
  },

  chat: {
    conversations: (params) => get('/conversations', params),
    conversation: (id) => get(`/conversations/${id}`),
    with: (userId) => get(`/conversations/with/${userId}`),
    messages: (id, params) => get(`/conversations/${id}/messages`, params),
    send: (body) => post('/conversations/messages', body),
    markRead: (id) => patch(`/conversations/${id}/read`),
    unreadCount: () => get('/conversations/unread-count'),
  },

  notifications: {
    inbox: (params) => get('/notifications', params),
    unreadCount: () => get('/notifications/unread-count'),
    markRead: (id) => patch(`/notifications/${id}/read`),
    markAllRead: () => patch('/notifications/read-all'),
    clearRead: () => remove('/notifications/read'),
    delete: (id) => remove(`/notifications/${id}`),
  },

  billing: {
    plans: () => get('/plans'),
    createPlan: (body) => post('/plans', body),
    updatePlan: (id, body) => put(`/plans/${id}`, body),
    withdrawPlan: (id) => patch(`/plans/${id}/withdraw`),
    createOrder: (planId) => post('/orders', { planId }),
    orders: (params) => get('/orders', params),
    verify: (body) => post('/payments/verify', body),
    currentSubscription: () => get('/subscriptions/current'),
    subscriptions: (params) => get('/subscriptions', params),
    cancel: (reason) => remove('/subscriptions/current', { reason }),
  },
}

# Endpoint reference

Every endpoint the application uses, grouped by the service that owns it. Swagger UI in each
service is the live version of this; the table is here so the routing is visible in one place.

All responses are wrapped:

```json
{ "success": true, "message": "...", "data": { }, "timestamp": "2026-01-01T00:00:00Z" }
```

Failures use the same envelope with `success: false` and an `error` object carrying a stable
`code` (`NOT_FOUND`, `VALIDATION_FAILED`, `FORBIDDEN`, …) and, for validation, `fieldErrors`.

Paged responses put `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` and
`empty` in `data`.

---

## user-service — port 8081

Nginx routes `/api/v1/{auth,admin,institutions,academics,profiles,plans,orders,payments,subscriptions}` here.

### Authentication `/api/v1/auth`
| Method | Path | Who |
| --- | --- | --- |
| POST | `/register` | anyone |
| POST | `/verify-otp` | anyone |
| POST | `/resend-otp` | anyone |
| POST | `/login` | anyone |
| POST | `/refresh` | anyone |
| POST | `/logout` | anyone |
| POST | `/forgot-password` | anyone |
| POST | `/reset-password` | anyone |
| POST | `/change-password` | signed in |
| GET | `/me` | signed in |

### Account administration `/api/v1/admin/users`
Administrator only.

| Method | Path |
| --- | --- |
| GET | `/` (search by query, status, role, institution) |
| GET | `/statistics`, `/statuses`, `/audit-logs` |
| GET | `/{id}`, `/{id}/audit-logs`, `/{id}/login-history` |
| POST | `/` (create staff or admin), `/{id}/force-password-reset` |
| PATCH | `/{id}/approve`, `/{id}/suspend`, `/{id}/reactivate` |

### Institutions `/api/v1/institutions`
`GET /branding` and `GET /{id}/branding` are public — the sign-up page needs them before anyone
has a token. Everything else is staff or administrator.

`GET /`, `GET /{id}`, `GET /by-code/{code}`, `POST /`, `PUT /{id}`,
`PATCH /{id}/deactivate`, `PATCH /{id}/reactivate`.

### Academics `/api/v1/academics/{type}`
`{type}` is `departments`, `programs`, `branches` or `batches`.

`GET /`, `GET /{id}`, `GET /placement-open` (batches), `POST /`, `PUT /{id}`,
`PATCH /{id}/deactivate`, `PATCH /{id}/reactivate`.

### Profiles `/api/v1/profiles`
`GET|PUT /me/student`, `GET|PUT /me/alumni`, `GET /students`, `GET /alumni`, `GET /mentors`,
`GET /students/{userId}`, `GET /alumni/{userId}`, `GET /alumni/companies`,
`GET /skills/suggest`, `GET /skills/popular`, `PATCH /skills/{id}/approve`.

### Billing
`GET /api/v1/plans` and `/plans/{id}` are public. `POST|PUT /plans`, `PATCH /plans/{id}/withdraw`
are administrator only.

`POST /api/v1/orders`, `GET /api/v1/orders`, `POST /api/v1/payments/verify`,
`GET /api/v1/subscriptions`, `GET /api/v1/subscriptions/current`,
`DELETE /api/v1/subscriptions/current`.

### Internal `/internal/v1`
Not exposed by Nginx. Called by the other two services.

`GET /users/{id}`, `POST /users/bulk`, `GET /profiles/{id}/eligibility`,
`GET /profiles/referrers`, `POST /profiles/{id}/placed`, `GET /profiles/stats`.

---

## career-service — port 8082

Nginx routes `/api/v1/{jobs,applications,resumes,ai,analytics}` here.

### Jobs `/api/v1/jobs`
`GET /search` (public board), `GET /manage`, `GET /mine`, `GET /{id}`,
`POST /`, `PUT /{id}`, `PATCH /{id}/publish|close|reopen`.

Companies live under the same prefix: `GET /companies`, `GET /companies/active`,
`GET /companies/{id}`, `POST /companies`, `PUT /companies/{id}`,
`PATCH /companies/{id}/verify|deactivate`.

### Applications `/api/v1/applications`
`POST /jobs/{jobId}` (apply), `GET /me`, `GET /me/{id}`, `PATCH /me/{id}/withdraw`,
`GET /jobs/{jobId}` (applicant list), `GET /{id}/review`, `PATCH /{id}/status`.

Referrals: `GET /{id}/referrers`, `POST /{id}/referrals`, `GET /referrals/received`,
`GET /referrals/sent`, `PATCH /referrals/{id}/respond`, `PATCH /referrals/{id}/withdraw`.

### Resumes `/api/v1/resumes`
`POST /` (multipart), `GET /`, `GET /{id}`, `GET /{id}/download`,
`PATCH /{id}/primary`, `PATCH /{id}/rename`, `DELETE /{id}`.

### AI `/api/v1/ai`
`POST /resumes/{id}/analyze`, `GET /analyses/{id}`, `GET /resumes/{id}/analysis`,
`GET /analyses`, `POST /resume-builder`, `POST /interview/questions`, `POST /interview/feedback`.

Analysis returns an id immediately; the client polls it until the status is `COMPLETED` or
`FAILED`.

### Analytics `/api/v1/analytics`
`GET /dashboard`, `POST /dashboard/refresh`, `GET /export/applications`,
`GET /export/placement-register` (staff and administrator, returns CSV).

---

## social-service — port 8083

Nginx routes `/api/v1/{posts,network,conversations,notifications}` and `/ws` here.

### Feed `/api/v1/posts`
`POST /`, `GET /feed`, `GET /search`, `GET /saved`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`,
`GET /author/{id}`, `GET /hashtag/{tag}`, `GET /hashtags/trending`, `GET /hashtags/suggest`,
`GET|POST /{id}/comments`, `GET|POST /comments/{id}/replies`, `DELETE /comments/{id}`,
`POST /{id}/reactions`, `POST /{id}/save`.

### Network `/api/v1/network`
`POST /requests`, `GET /requests/received`, `GET /requests/sent`,
`PATCH /requests/{id}/accept|reject|withdraw`, `GET /connections`,
`DELETE /connections/{id}`, `GET /summary`, `GET /suggestions`, `GET /feed`.

### Chat `/api/v1/conversations`
`GET /`, `GET /unread-count`, `GET /with/{userId}`, `GET /{id}/messages`,
`POST /messages`, `PATCH /{id}/read`, `DELETE /messages/{id}`.

### Notifications `/api/v1/notifications`
`GET /`, `GET /unread-count`, `PATCH /{id}/read`, `PATCH /read-all`, `DELETE /read`.

### WebSocket `/ws`
One STOMP endpoint. The token goes in the `Authorization` header of the CONNECT frame.

- Subscribe to `/topic/conversations/{conversationId}` for messages.
- Subscribe to `/topic/notifications/{userId}` for the bell.
- Publish to `/app/chat.send` to send a message over the socket instead of over HTTP.

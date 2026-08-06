# Module ownership

Six members, six slices. **Every file belongs to exactly one person.** If you need something in
someone else's file, ask them to add it — do not edit it yourself. That single rule is what stops
merge conflicts.

Replace "Member N" with real names before the first push.

| # | Member | Slice | Backend files | Frontend files |
|---|---|---|---|---|
| 1 | Vaishnavi | Auth & accounts | `user-service` → `controller/AuthController`, `service/AuthService`, `EmailService`, `entity/{User,RefreshToken,OtpToken}`, matching repositories, `dto/AuthDtos` | `pages/{Login,Register,VerifyOtp,ForgotPassword,ResetPassword,ChangePassword,Landing}`, the `api.auth` block in `lib/api.js` |
| 2 | Member 2 | Profiles, institution, academics | `user-service` → `ProfileController`, `InstitutionController`, `AcademicController`, their services, `entity/{StudentProfile,AlumniProfile,Institution,AcademicUnit,Skill}`, `dto/{ProfileDtos,InstitutionDtos,AcademicDtos}` | `pages/{Home,Profile,PublicProfile,Directory,Academics}`, the profile/institution blocks in `lib/api.js` |
| 3 | Member 3 | Jobs & applications | `career-service` → `JobController`, `ApplicationController`, their services, `entity/{Job,JobApplication,Company,ReferralRequest}`, `dto/{JobDtos,ApplicationDtos,CompanyDtos}` | `pages/{Jobs,JobDetail,JobForm,ManageJobs,JobApplicants,Applications,Referrals}`, the job/application blocks in `lib/api.js` |
| 4 | Member 4 | Resumes, AI, analytics | `career-service` → `ResumeController`, `AiController`, `AnalyticsController`, `ResumeService`, `AiService`, `GeminiClient`, `entity/{Resume,ResumeAnalysis}`, `dto/{ResumeDtos,AiDtos,AnalyticsDtos}` | `pages/{Resumes,AiTools}`, the resume/AI blocks in `lib/api.js` |
| 5 | Member 5 | Feed & network | `social-service` → `FeedController`, `NetworkController`, `PostService`, `ConnectionService`, `entity/{Post,Comment,Connection}` | `pages/{Feed,PostDetail,Network}`, the feed/network blocks in `lib/api.js` |
| 6 | Member 6 | Chat, notifications, premium, admin | `social-service` → `ChatController`, `NotificationController`, `ChatService`, `WebSocketConfig`, `entity/{Conversation,Message,Notification}`; `user-service` → `BillingController`, `AdminUserController` and their services | `pages/{Messages,Notifications,Premium,Admin,AdminUser}`, the chat/notification/billing/admin blocks in `lib/api.js` |
| — | Prathamesh (lead) | Shared code | `backend/common`, `api-gateway`, all POMs, Dockerfiles, `application.yml`, `docker-compose.yml`, CI | `lib/api.js`, `lib/auth.jsx`, `lib/toast.jsx`, `lib/format.js`, `lib/realtime.js`, `components/`, `App.jsx`, `package.json` |

## The shared files

`App.jsx`, `components/Layout.jsx`, `lib/api.js` and `backend/common/**` are the files more than
one person touches.

- **`lib/api.js`** — every endpoint lives here, grouped by feature (`api.auth`, `api.jobs`,
  `api.feed`, ...). Add functions inside *your* group only, in one small commit. Two people adding
  to two different groups merge without a conflict; two people reformatting the file do not.
- **`App.jsx` / `Layout.jsx`** — all 30 routes and the nav are already wired. You normally change
  nothing here; the route for your page already points at your file.
- **`backend/common`** — changes go through the lead. If you need a new `ErrorCode` or a helper,
  ask rather than adding it yourself; a change there rebuilds everyone's service.

## Pasting finished code in

Paths and names match the working project, so:

- a page → overwrite `frontend/src/pages/<Name>.jsx`, delete nothing else
- a controller/service/entity → drop it in the matching package, the imports already resolve
- a class that lives in a service root package (`DataSeeder`, `WebSocketConfig`) → drop it next to
  the `*Application.java` file

The only file you may need to touch afterwards is `lib/api.js`, if your page calls an endpoint
whose function is not there yet.

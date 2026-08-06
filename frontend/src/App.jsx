import { Route, Routes } from 'react-router-dom'
import { RequireAuth, RequireRole, RequirePremium } from './components/Guards'
import Layout from './components/Layout'

import Academics from './pages/Academics'
import Admin from './pages/Admin'
import AdminUser from './pages/AdminUser'
import AiTools from './pages/AiTools'
import Applications from './pages/Applications'
import ChangePassword from './pages/ChangePassword'
import Directory from './pages/Directory'
import Feed from './pages/Feed'
import ForgotPassword from './pages/ForgotPassword'
import Home from './pages/Home'
import JobApplicants from './pages/JobApplicants'
import JobDetail from './pages/JobDetail'
import JobForm from './pages/JobForm'
import Jobs from './pages/Jobs'
import Landing from './pages/Landing'
import Login from './pages/Login'
import ManageJobs from './pages/ManageJobs'
import Messages from './pages/Messages'
import Network from './pages/Network'
import NotFound from './pages/NotFound'
import Notifications from './pages/Notifications'
import PostDetail from './pages/PostDetail'
import Premium from './pages/Premium'
import Profile from './pages/Profile'
import PublicProfile from './pages/PublicProfile'
import Referrals from './pages/Referrals'
import Register from './pages/Register'
import ResetPassword from './pages/ResetPassword'
import Resumes from './pages/Resumes'
import VerifyOtp from './pages/VerifyOtp'

const STAFF = ['INSTITUTION_STAFF', 'PLATFORM_ADMIN']
const POSTERS = ['INSTITUTION_STAFF', 'ALUMNI', 'PLATFORM_ADMIN']

/**
 * Every route in the application.
 *
 * Public routes sit at the top; everything else renders inside {@link Layout}, which is wrapped
 * once in {@link RequireAuth} rather than each child repeating the check. Role checks stay on the
 * few routes that need them.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/welcome" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/verify" element={<VerifyOtp />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route
        path="/change-password"
        element={
          <RequireAuth>
            <ChangePassword />
          </RequireAuth>
        }
      />

      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Home />} />
        <Route path="/feed" element={<Feed />} />
        <Route path="/feed/:postId" element={<PostDetail />} />
        <Route path="/network" element={<Network />} />
        <Route path="/directory" element={<Directory />} />
        <Route path="/messages" element={<Messages />} />
        <Route path="/messages/:conversationId" element={<Messages />} />
        <Route path="/notifications" element={<Notifications />} />

        <Route path="/jobs" element={<Jobs />} />
        <Route path="/jobs/new" element={<RequireRole roles={POSTERS}><JobForm /></RequireRole>} />
        <Route path="/jobs/manage" element={<RequireRole roles={POSTERS}><ManageJobs /></RequireRole>} />
        <Route path="/jobs/:jobId" element={<JobDetail />} />
        <Route path="/jobs/:jobId/edit" element={<RequireRole roles={POSTERS}><JobForm /></RequireRole>} />
        <Route
          path="/jobs/:jobId/applicants"
          element={<RequireRole roles={POSTERS}><JobApplicants /></RequireRole>}
        />

        <Route path="/applications" element={<Applications />} />
        <Route path="/applications/:applicationId" element={<Applications />} />
        <Route path="/referrals" element={<Referrals />} />
        <Route path="/resumes" element={<Resumes />} />
        <Route path="/resume/analysis/:analysisId" element={<Resumes />} />
        <Route path="/ai" element={<RequirePremium><AiTools /></RequirePremium>} />

        <Route path="/profile" element={<Profile />} />
        <Route path="/people/:userId" element={<PublicProfile />} />

        <Route path="/premium" element={<Premium />} />
        <Route path="/premium/billing" element={<Premium />} />

        <Route path="/academics" element={<RequireRole roles={STAFF}><Academics /></RequireRole>} />
        <Route path="/admin" element={<RequireRole roles={['PLATFORM_ADMIN']}><Admin /></RequireRole>} />
        <Route
          path="/admin/users/:userId"
          element={<RequireRole roles={['PLATFORM_ADMIN']}><AdminUser /></RequireRole>}
        />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}

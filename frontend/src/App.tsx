import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'

// Auth pages
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

// Admin pages
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminComplaintsPage from './pages/admin/AdminComplaintsPage'
import AdminWorkersPage from './pages/admin/AdminWorkersPage'
import AdminRoutesPage from './pages/admin/AdminRoutesPage'

// Citizen pages
import CitizenDashboard from './pages/citizen/CitizenDashboard'
import CitizenComplaintsPage from './pages/citizen/CitizenComplaintsPage'
import NewComplaintPage from './pages/citizen/NewComplaintPage'
import RewardsDashboard from './pages/citizen/RewardsDashboard'

// Worker pages
import WorkerDashboard from './pages/worker/WorkerDashboard'
import WorkerTasksPage from './pages/worker/WorkerTasksPage'
import WorkerRoutesPage from './pages/worker/WorkerRoutesPage'

// Recycler pages
import RecyclerDashboard from './pages/recycler/RecyclerDashboard'
import RecyclerMarketplacePage from './pages/recycler/RecyclerMarketplacePage'

// Ye imports add karo
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'

// Placeholder for pages not yet implemented
const Placeholder = ({ title }: { title: string }) => (
  <div className="flex items-center justify-center h-64 text-gray-400 text-lg">{title} — Coming soon</div>
)

export default function App() {
  const { isAuthenticated, user } = useAuthStore()

  return (
    <Routes>
      {/* Public routes */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to={`/${user?.role.toLowerCase()}/dashboard`} /> : <LoginPage />}
      />
      <Route
        path="/register"
        element={isAuthenticated ? <Navigate to={`/${user?.role.toLowerCase()}/dashboard`} /> : <RegisterPage />}
      />
      <Route path="/unauthorized" element={<Placeholder title="403 — Unauthorized" />} />

      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      {/* Admin routes */}
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <Layout>
              <Routes>
                <Route path="dashboard" element={<AdminDashboard />} />
                <Route path="complaints" element={<AdminComplaintsPage />} />
                <Route path="workers" element={<AdminWorkersPage />} />
                <Route path="routes" element={<AdminRoutesPage />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Citizen routes */}
      <Route
        path="/citizen/*"
        element={
          <ProtectedRoute allowedRoles={['CITIZEN']}>
            <Layout>
              <Routes>
                <Route path="dashboard" element={<CitizenDashboard />} />
                <Route path="complaints" element={<CitizenComplaintsPage />} />
                <Route path="complaints/new" element={<NewComplaintPage />} />
                <Route path="rewards" element={<RewardsDashboard />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Worker routes */}
      <Route
        path="/worker/*"
        element={
          <ProtectedRoute allowedRoles={['WORKER']}>
            <Layout>
              <Routes>
                <Route path="dashboard" element={<WorkerDashboard />} />
                <Route path="tasks" element={<WorkerTasksPage />} />
                <Route path="routes" element={<WorkerRoutesPage />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Recycler routes */}
      <Route
        path="/recycler/*"
        element={
          <ProtectedRoute allowedRoles={['RECYCLER']}>
            <Layout>
              <Routes>
                <Route path="dashboard" element={<RecyclerDashboard />} />
                <Route path="marketplace" element={<RecyclerMarketplacePage />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Default redirect */}
      <Route
        path="/"
        element={
          isAuthenticated
            ? <Navigate to={`/${user?.role.toLowerCase()}/dashboard`} />
            : <Navigate to="/login" />
        }
      />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

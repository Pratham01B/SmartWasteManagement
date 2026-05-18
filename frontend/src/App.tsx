import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import AdminDashboard from './pages/admin/AdminDashboard'

// Placeholder pages — replace with full implementations
const Placeholder = ({ title }: { title: string }) => (
  <div className="flex items-center justify-center h-64 text-gray-400 text-lg">{title} — Coming soon</div>
)

export default function App() {
  const { isAuthenticated, user } = useAuthStore()

  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={isAuthenticated ? <Navigate to={`/${user?.role.toLowerCase()}/dashboard`} /> : <LoginPage />} />
      <Route path="/register" element={isAuthenticated ? <Navigate to={`/${user?.role.toLowerCase()}/dashboard`} /> : <RegisterPage />} />
      <Route path="/unauthorized" element={<Placeholder title="403 — Unauthorized" />} />

      {/* Admin routes */}
      <Route path="/admin/*" element={
        <ProtectedRoute allowedRoles={['ADMIN']}>
          <Layout>
            <Routes>
              <Route path="dashboard" element={<AdminDashboard />} />
              <Route path="complaints" element={<Placeholder title="Complaint Management" />} />
              <Route path="workers" element={<Placeholder title="Worker Management" />} />
              <Route path="routes" element={<Placeholder title="Route Optimizer" />} />
            </Routes>
          </Layout>
        </ProtectedRoute>
      } />

      {/* Citizen routes */}
      <Route path="/citizen/*" element={
        <ProtectedRoute allowedRoles={['CITIZEN']}>
          <Layout>
            <Routes>
              <Route path="dashboard" element={<Placeholder title="Citizen Dashboard" />} />
              <Route path="complaints" element={<Placeholder title="My Complaints" />} />
              <Route path="complaints/new" element={<Placeholder title="File a Complaint" />} />
              <Route path="rewards" element={<Placeholder title="Rewards" />} />
            </Routes>
          </Layout>
        </ProtectedRoute>
      } />

      {/* Worker routes */}
      <Route path="/worker/*" element={
        <ProtectedRoute allowedRoles={['WORKER']}>
          <Layout>
            <Routes>
              <Route path="dashboard" element={<Placeholder title="Worker Dashboard" />} />
              <Route path="tasks" element={<Placeholder title="My Tasks" />} />
              <Route path="routes" element={<Placeholder title="My Routes" />} />
            </Routes>
          </Layout>
        </ProtectedRoute>
      } />

      {/* Recycler routes */}
      <Route path="/recycler/*" element={
        <ProtectedRoute allowedRoles={['RECYCLER']}>
          <Layout>
            <Routes>
              <Route path="dashboard" element={<Placeholder title="Recycler Dashboard" />} />
              <Route path="marketplace" element={<Placeholder title="Marketplace" />} />
            </Routes>
          </Layout>
        </ProtectedRoute>
      } />

      {/* Default redirect */}
      <Route path="/" element={
        isAuthenticated
          ? <Navigate to={`/${user?.role.toLowerCase()}/dashboard`} />
          : <Navigate to="/login" />
      } />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import type { Role } from '../types'

interface Props {
  children: React.ReactNode
  allowedRoles?: Role[]
}

/**
 * Wraps a route — redirects to /login if not authenticated,
 * or to /unauthorized if the user's role isn't allowed.
 */
export default function ProtectedRoute({ children, allowedRoles }: Props) {
  const { isAuthenticated, user } = useAuthStore()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />
  }

  return <>{children}</>
}

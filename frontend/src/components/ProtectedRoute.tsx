import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

/**
 * Gates nested routes behind authentication. While the initial session check is in flight
 * it renders a loading placeholder; unauthenticated users are redirected to /login.
 */
export function ProtectedRoute() {
  const { user, loading } = useAuth()

  if (loading) {
    return <p style={{ padding: '1rem' }}>Loading…</p>
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}

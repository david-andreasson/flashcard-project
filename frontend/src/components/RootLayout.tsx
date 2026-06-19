import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function RootLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function onLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <div>
      <nav
        style={{
          padding: '1rem',
          borderBottom: '1px solid #ccc',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <span style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <Link to="/" style={{ fontWeight: 600 }}>Flashcard App</Link>
          {user && <Link to="/courses">Courses</Link>}
          {user && <Link to="/study-history">History</Link>}
        </span>
        {user && (
          <span style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            <span style={{ color: '#666' }}>
              {user.email}
              {user.role === 'ADMIN' && ' (admin)'}
            </span>
            <button onClick={onLogout}>Log out</button>
          </span>
        )}
      </nav>
      <main style={{ padding: '1rem' }}>
        <Outlet />
      </main>
    </div>
  )
}

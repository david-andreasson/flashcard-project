import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ThemeSwitcher } from './ThemeSwitcher'
import { Button } from './ui'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm no-underline transition hover:no-underline hover:text-ink ${
    isActive ? 'font-medium text-accent' : 'text-muted'
  }`

export function RootLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function onLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen">
      <nav className="sticky top-0 z-20 flex flex-wrap items-center justify-between gap-3 border-b border-line bg-surface px-4 py-3 shadow-[var(--shadow)]">
        <div className="flex flex-wrap items-center gap-4">
          <Link to="/" className="text-base font-medium text-ink no-underline hover:no-underline">
            Flashcard App
          </Link>
          {user && <NavLink to="/courses" className={navLinkClass}>Courses</NavLink>}
          {user && <NavLink to="/ai/generate" className={navLinkClass}>Generate cards</NavLink>}
          {user && <NavLink to="/progress" className={navLinkClass}>Progress</NavLink>}
          {user && <NavLink to="/study-history" className={navLinkClass}>History</NavLink>}
        </div>
        <div className="flex items-center gap-4">
          <ThemeSwitcher />
          {user && (
            <div className="flex items-center gap-3">
              <span className="text-sm text-muted">
                {user.email}
                {user.role === 'ADMIN' && ' (admin)'}
              </span>
              <Button variant="secondary" onClick={onLogout} className="px-3 py-1.5">
                Log out
              </Button>
            </div>
          )}
        </div>
      </nav>
      <main className="mx-auto w-full max-w-3xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

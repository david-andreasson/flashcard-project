import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="text-center">
      <h1 className="text-2xl font-medium text-ink">404 — page not found</h1>
      <p className="mt-2 text-muted">The page you're looking for doesn't exist.</p>
      <Link to="/" className="mt-4 inline-block">
        Back home
      </Link>
    </div>
  )
}

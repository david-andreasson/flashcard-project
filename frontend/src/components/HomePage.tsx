import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const tiles = [
  { to: '/courses', title: 'Courses', desc: 'Browse your courses and decks, or study a deck.' },
  { to: '/ai/generate', title: 'Generate cards', desc: 'Turn pasted text or a PDF into flashcards.' },
  { to: '/progress', title: 'Progress', desc: 'See what is due and what you have reviewed.' },
  { to: '/study-history', title: 'History', desc: 'Look back at your past study sessions.' },
]

export function HomePage() {
  const { user } = useAuth()
  return (
    <div>
      <h1 className="text-2xl font-medium text-ink">Flashcard Study App</h1>
      <p className="mt-2 text-muted">
        {user ? (
          <>
            Signed in as <span className="text-ink">{user.email}</span> · plan {user.plan}.
          </>
        ) : (
          'Welcome!'
        )}
      </p>
      <div className="mt-6 grid gap-3 sm:grid-cols-2">
        {tiles.map((t) => (
          <Link
            key={t.to}
            to={t.to}
            className="block rounded-xl border border-line bg-surface p-5 no-underline transition hover:border-accent hover:no-underline"
          >
            <div className="font-medium text-ink">{t.title}</div>
            <div className="mt-1 text-sm text-muted">{t.desc}</div>
          </Link>
        ))}
      </div>
    </div>
  )
}

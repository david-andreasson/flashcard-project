import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { user } = useAuth()
  return (
    <div>
      <h1>Flashcard Study App</h1>
      {user ? (
        <p>
          Signed in as <strong>{user.email}</strong> — plan: {user.plan}.
        </p>
      ) : (
        <p>Welcome!</p>
      )}
      <p>Courses and decks are coming in the next changes.</p>
    </div>
  )
}

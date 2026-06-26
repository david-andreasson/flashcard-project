import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getDueCards, reviewCard, type Grade } from '../lib/study'
import type { Card } from '../lib/courses'
import { Alert, Button } from './ui'

const GRADES: { grade: Grade; label: string; className: string }[] = [
  { grade: 'AGAIN', label: 'Again', className: 'text-danger' },
  { grade: 'HARD', label: 'Hard', className: 'text-muted' },
  { grade: 'GOOD', label: 'Good', className: 'text-accent' },
  { grade: 'EASY', label: 'Easy', className: 'text-success' },
]

export function ReviewPage() {
  const { courseId, deckId } = useParams()
  const cId = Number(courseId)
  const dId = Number(deckId)
  const navigate = useNavigate()

  const [queue, setQueue] = useState<Card[] | null>(null) // null = loading
  const [reviewed, setReviewed] = useState(0)
  const [showBack, setShowBack] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setQueue(null)
    setReviewed(0)
    setShowBack(false)
    setError(null)
    getDueCards(cId, dId)
      .then(setQueue)
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load due cards'))
  }, [cId, dId])

  useEffect(() => {
    load()
  }, [load])

  async function grade(g: Grade) {
    if (!queue || queue.length === 0 || submitting) return
    const card = queue[0]
    setSubmitting(true)
    setError(null)
    try {
      await reviewCard(cId, dId, card.id, g)
      setQueue((q) => (q ? q.slice(1) : q))
      setReviewed((n) => n + 1)
      setShowBack(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save review')
    } finally {
      setSubmitting(false)
    }
  }

  if (error && !queue) {
    return (
      <div>
        <h1 className="text-2xl font-medium text-ink">Can't review this deck</h1>
        <div className="mt-3">
          <Alert tone="danger">{error}</Alert>
        </div>
        <Button onClick={() => navigate(`/courses/${cId}/decks/${dId}`)} className="mt-4">
          Back to deck
        </Button>
      </div>
    )
  }
  if (!queue) return <p className="text-muted">Loading…</p>

  if (queue.length === 0) {
    return (
      <div>
        <h1 className="text-2xl font-medium text-ink">{reviewed > 0 ? 'Review complete' : 'Nothing due'}</h1>
        <p className="mt-2 text-muted">
          {reviewed > 0
            ? `Reviewed ${reviewed} card${reviewed === 1 ? '' : 's'}.`
            : 'No cards are due in this deck right now.'}
        </p>
        <div className="mt-4 flex gap-2">
          <Button onClick={load}>Refresh</Button>
          <Button onClick={() => navigate(`/courses/${cId}/decks/${dId}`)}>Back to deck</Button>
        </div>
      </div>
    )
  }

  const card = queue[0]
  return (
    <div>
      <Link
        to={`/courses/${cId}`}
        className="mb-3 inline-block text-sm text-muted no-underline hover:text-ink"
      >
        ← Back to course
      </Link>
      <div className="mb-2 text-sm text-muted">
        {queue.length} due · {reviewed} reviewed
      </div>
      <div className="flex min-h-40 flex-col items-center justify-center rounded-xl border border-line bg-surface p-8 text-center">
        <div className="text-2xl font-medium text-ink">{card.front}</div>
        {showBack && (
          <>
            <hr className="my-5 w-full border-0 border-t border-line" />
            <div className="text-xl text-ink">{card.back}</div>
            {card.notes && <div className="mt-2 text-muted">{card.notes}</div>}
          </>
        )}
      </div>

      {error && (
        <p role="alert" className="mt-2 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-4 flex flex-wrap justify-center gap-2">
        {!showBack ? (
          <Button variant="primary" onClick={() => setShowBack(true)} className="px-6">
            Show answer
          </Button>
        ) : (
          GRADES.map((g) => (
            <Button
              key={g.grade}
              onClick={() => grade(g.grade)}
              disabled={submitting}
              className={`px-5 ${g.className}`}
            >
              {g.label}
            </Button>
          ))
        )}
      </div>
    </div>
  )
}

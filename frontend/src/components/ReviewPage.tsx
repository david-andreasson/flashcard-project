import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getDueCards, reviewCard, type Grade } from '../lib/study'
import type { Card } from '../lib/courses'

const GRADES: { grade: Grade; label: string; color: string }[] = [
  { grade: 'AGAIN', label: 'Again', color: 'crimson' },
  { grade: 'HARD', label: 'Hard', color: '#b58900' },
  { grade: 'GOOD', label: 'Good', color: '#268bd2' },
  { grade: 'EASY', label: 'Easy', color: 'green' },
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
      <div style={{ maxWidth: 640 }}>
        <h1>Can't review this deck</h1>
        <p role="alert" style={{ color: 'crimson' }}>{error}</p>
        <button onClick={() => navigate(`/courses/${cId}/decks/${dId}`)}>Back to deck</button>
      </div>
    )
  }
  if (!queue) return <p style={{ padding: '1rem' }}>Loading…</p>

  if (queue.length === 0) {
    return (
      <div style={{ maxWidth: 640 }}>
        <h1>{reviewed > 0 ? 'Review complete' : 'Nothing due'}</h1>
        <p style={{ color: '#888' }}>
          {reviewed > 0
            ? `Reviewed ${reviewed} card${reviewed === 1 ? '' : 's'}.`
            : 'No cards are due in this deck right now.'}
        </p>
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
          <button onClick={load}>Refresh</button>
          <button onClick={() => navigate(`/courses/${cId}/decks/${dId}`)}>Back to deck</button>
        </div>
      </div>
    )
  }

  const card = queue[0]
  return (
    <div style={{ maxWidth: 640 }}>
      <div style={{ color: '#888', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
        {queue.length} due · {reviewed} reviewed
      </div>
      <div
        style={{
          border: '1px solid #ccc',
          borderRadius: 10,
          padding: '2rem',
          minHeight: 160,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
        }}
      >
        <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{card.front}</div>
        {showBack && (
          <>
            <hr style={{ width: '100%', margin: '1.25rem 0', border: 0, borderTop: '1px solid #ddd' }} />
            <div style={{ fontSize: '1.3rem' }}>{card.back}</div>
            {card.notes && <div style={{ color: '#888', marginTop: '0.5rem' }}>{card.notes}</div>}
          </>
        )}
      </div>

      {error && <p role="alert" style={{ color: 'crimson', marginTop: '0.5rem' }}>{error}</p>}

      <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'center', gap: '0.5rem' }}>
        {!showBack ? (
          <button onClick={() => setShowBack(true)} style={{ padding: '0.6rem 1.5rem' }}>
            Show answer
          </button>
        ) : (
          GRADES.map((g) => (
            <button
              key={g.grade}
              onClick={() => grade(g.grade)}
              disabled={submitting}
              style={{ padding: '0.6rem 1.1rem', color: g.color }}
            >
              {g.label}
            </button>
          ))
        )}
      </div>
    </div>
  )
}

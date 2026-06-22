import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { listCards, type Card } from '../lib/courses'
import { recordSession } from '../lib/study'

function shuffle<T>(items: T[]): T[] {
  const a = [...items]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

export function StudyPage() {
  const { courseId, deckId } = useParams()
  const cId = Number(courseId)
  const dId = Number(deckId)
  const navigate = useNavigate()

  const [queue, setQueue] = useState<Card[] | null>(null) // null = loading
  const [totalCards, setTotalCards] = useState(0)
  const [showBack, setShowBack] = useState(false)
  const [finished, setFinished] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [recordError, setRecordError] = useState<string | null>(null)
  const missed = useRef<Set<number>>(new Set())

  const load = useCallback(() => {
    setQueue(null)
    setFinished(false)
    setShowBack(false)
    missed.current = new Set()
    listCards(cId, dId, 0, 500)
      .then((p) => {
        const cards = shuffle(p.content)
        setTotalCards(cards.length)
        setQueue(cards)
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load deck'))
  }, [cId, dId])

  useEffect(() => {
    load()
  }, [load])

  const correctCount = totalCards - missed.current.size

  // Finish when the queue empties (every card answered correctly).
  useEffect(() => {
    if (queue && totalCards > 0 && queue.length === 0 && !finished) {
      setFinished(true)
      recordSession({ deckId: dId, totalCards, correctCount: totalCards - missed.current.size })
        .catch((e) => setRecordError(e instanceof Error ? e.message : 'Could not save session'))
    }
  }, [queue, totalCards, finished, dId])

  function grade(correct: boolean) {
    setQueue((q) => {
      if (!q || q.length === 0) return q
      const [card, ...rest] = q
      if (correct) {
        return rest // done with this card
      }
      missed.current.add(card.id)
      return [...rest, card] // round-robin: back of the queue
    })
    setShowBack(false)
  }

  if (error) {
    return (
      <div>
        <h1>Can't study this deck</h1>
        <button onClick={() => navigate('/courses')}>Back to courses</button>
      </div>
    )
  }
  if (!queue) return <p style={{ padding: '1rem' }}>Loading…</p>

  if (totalCards === 0) {
    return (
      <div style={{ maxWidth: 640 }}>
        <h1>Nothing to study</h1>
        <p style={{ color: '#888' }}>This deck has no cards yet.</p>
        <button onClick={() => navigate(`/courses/${cId}/decks/${dId}`)}>Back to deck</button>
      </div>
    )
  }

  // Show the summary as soon as the queue empties. The session-recording effect runs after this
  // render, so guarding only on `finished` would let this render reach `queue[0]` (undefined) and
  // crash on `card.front`; checking `queue.length === 0` covers that one render.
  if (finished || queue.length === 0) {
    return (
      <div style={{ maxWidth: 640 }}>
        <h1>Session complete</h1>
        <p style={{ fontSize: '1.1rem' }}>
          {totalCards} cards · <strong>{correctCount}</strong> correct on first try
        </p>
        {recordError && (
          <p role="alert" style={{ color: '#b58900' }}>Session not saved: {recordError}</p>
        )}
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
          <button onClick={load}>Study again</button>
          <button onClick={() => navigate(`/courses/${cId}/decks/${dId}`)}>Back to deck</button>
        </div>
      </div>
    )
  }

  const card = queue[0]
  return (
    <div style={{ maxWidth: 640 }}>
      <div style={{ color: '#888', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
        {queue.length} card{queue.length === 1 ? '' : 's'} left
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

      <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'center', gap: '0.75rem' }}>
        {!showBack ? (
          <button onClick={() => setShowBack(true)} style={{ padding: '0.6rem 1.5rem' }}>
            Show answer
          </button>
        ) : (
          <>
            <button onClick={() => grade(false)} style={{ padding: '0.6rem 1.5rem', color: 'crimson' }}>
              ✗ Missed
            </button>
            <button onClick={() => grade(true)} style={{ padding: '0.6rem 1.5rem', color: 'green' }}>
              ✓ Got it
            </button>
          </>
        )}
      </div>
    </div>
  )
}

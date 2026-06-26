import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { listCards, type Card } from '../lib/courses'
import { recordSession } from '../lib/study'
import { Button } from './ui'

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
        <h1 className="text-2xl font-medium text-ink">Can't study this deck</h1>
        <Button onClick={() => navigate('/courses')} className="mt-4">
          Back to courses
        </Button>
      </div>
    )
  }
  if (!queue) return <p className="text-muted">Loading…</p>

  if (totalCards === 0) {
    return (
      <div>
        <h1 className="text-2xl font-medium text-ink">Nothing to study</h1>
        <p className="mt-2 text-muted">This deck has no cards yet.</p>
        <Button onClick={() => navigate(`/courses/${cId}/decks/${dId}`)} className="mt-4">
          Back to deck
        </Button>
      </div>
    )
  }

  // Show the summary as soon as the queue empties. The session-recording effect runs after this
  // render, so guarding only on `finished` would let this render reach `queue[0]` (undefined) and
  // crash on `card.front`; checking `queue.length === 0` covers that one render.
  if (finished || queue.length === 0) {
    return (
      <div>
        <h1 className="text-2xl font-medium text-ink">Session complete</h1>
        <p className="mt-2 text-lg text-ink">
          {totalCards} cards · <span className="font-medium">{correctCount}</span> correct on first try
        </p>
        {recordError && (
          <p role="alert" className="mt-2 text-sm text-danger">
            Session not saved: {recordError}
          </p>
        )}
        <div className="mt-4 flex gap-2">
          <Button variant="primary" onClick={load}>
            Study again
          </Button>
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
        {queue.length} card{queue.length === 1 ? '' : 's'} left
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

      <div className="mt-4 flex justify-center gap-3">
        {!showBack ? (
          <Button variant="primary" onClick={() => setShowBack(true)} className="px-6">
            Show answer
          </Button>
        ) : (
          <>
            <Button onClick={() => grade(false)} className="px-6 text-danger">
              Missed
            </Button>
            <Button variant="primary" onClick={() => grade(true)} className="px-6">
              Got it
            </Button>
          </>
        )}
      </div>
    </div>
  )
}

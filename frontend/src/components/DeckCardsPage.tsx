import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import {
  createCard,
  deleteCard,
  getCourse,
  listCards,
  updateCard,
  type Card,
  type Course,
} from '../lib/courses'
import { Alert, Button, Input } from './ui'

interface EditState {
  id: number
  front: string
  back: string
  notes: string
}

export function DeckCardsPage() {
  const { courseId, deckId } = useParams()
  const cId = Number(courseId)
  const dId = Number(deckId)
  const { user } = useAuth()
  const navigate = useNavigate()

  const [course, setCourse] = useState<Course | null>(null)
  const [cards, setCards] = useState<Card[] | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [front, setFront] = useState('')
  const [back, setBack] = useState('')
  const [notes, setNotes] = useState('')
  const [editing, setEditing] = useState<EditState | null>(null)

  const isOwner = !!user && !!course && course.ownerId === user.id

  const loadCards = useCallback(() => {
    listCards(cId, dId).then((p) => setCards(p.content)).catch((e) => setError(e.message))
  }, [cId, dId])

  useEffect(() => {
    getCourse(cId)
      .then((c) => {
        setCourse(c)
        loadCards()
      })
      .catch(() => setNotFound(true))
  }, [cId, loadCards])

  if (notFound) {
    return (
      <div>
        <h1 className="text-2xl font-medium text-ink">Not found</h1>
        <Button onClick={() => navigate('/courses')} className="mt-4">
          Back to courses
        </Button>
      </div>
    )
  }
  if (!course || !cards) return <p className="text-muted">Loading…</p>

  async function onAdd(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await createCard(cId, dId, { front, back, notes: notes || null })
      setFront('')
      setBack('')
      setNotes('')
      loadCards()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not add card')
    }
  }

  async function onSaveEdit(event: FormEvent) {
    event.preventDefault()
    if (!editing) return
    try {
      await updateCard(cId, dId, editing.id, {
        front: editing.front,
        back: editing.back,
        notes: editing.notes || null,
      })
      setEditing(null)
      loadCards()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save card')
    }
  }

  async function onDelete(card: Card) {
    if (!window.confirm('Delete this card?')) return
    try {
      await deleteCard(cId, dId, card.id)
      loadCards()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete card')
    }
  }

  return (
    <div>
      <Button onClick={() => navigate(`/courses/${cId}`)} variant="ghost" className="mb-4 px-2 py-1">
        ← Back to course
      </Button>
      <h1 className="text-2xl font-medium text-ink">
        Cards
        {course.visibility === 'PUBLIC' && <span className="ml-2 text-sm text-muted">· public</span>}
      </h1>
      {error && (
        <div className="mt-3">
          <Alert tone="danger">{error}</Alert>
        </div>
      )}

      {cards.length === 0 ? (
        <p className="mt-3 text-muted">No cards yet.</p>
      ) : (
        <div className="mt-4 flex gap-2">
          <Button variant="primary" onClick={() => navigate(`/courses/${cId}/decks/${dId}/study`)}>
            Study this deck
          </Button>
          <Button onClick={() => navigate(`/courses/${cId}/decks/${dId}/review`)}>Review due</Button>
        </div>
      )}

      <ul className="mt-4 list-none p-0">
        {cards.map((card) => (
          <li key={card.id} className="mb-2 rounded-xl border border-line bg-surface p-4">
            {editing?.id === card.id ? (
              <form onSubmit={onSaveEdit} className="flex flex-col gap-2">
                <Input
                  value={editing.front}
                  onChange={(e) => setEditing({ ...editing, front: e.target.value })}
                  required
                  placeholder="Front"
                />
                <Input
                  value={editing.back}
                  onChange={(e) => setEditing({ ...editing, back: e.target.value })}
                  required
                  placeholder="Back"
                />
                <Input
                  value={editing.notes}
                  onChange={(e) => setEditing({ ...editing, notes: e.target.value })}
                  placeholder="Notes (optional)"
                />
                <div className="flex gap-2">
                  <Button type="submit" variant="primary">
                    Save
                  </Button>
                  <Button type="button" onClick={() => setEditing(null)}>
                    Cancel
                  </Button>
                </div>
              </form>
            ) : (
              <div className="flex items-start gap-3">
                <div className="flex-1">
                  <div className="font-medium text-ink">{card.front}</div>
                  <div className="text-muted">{card.back}</div>
                  {card.notes && <div className="mt-1 text-sm text-muted">{card.notes}</div>}
                </div>
                {isOwner && (
                  <div className="flex gap-2">
                    <Button
                      onClick={() =>
                        setEditing({ id: card.id, front: card.front, back: card.back, notes: card.notes ?? '' })
                      }
                      className="px-3 py-1.5"
                    >
                      Edit
                    </Button>
                    <Button onClick={() => onDelete(card)} className="px-3 py-1.5 text-danger">
                      Delete
                    </Button>
                  </div>
                )}
              </div>
            )}
          </li>
        ))}
      </ul>

      {isOwner && (
        <form onSubmit={onAdd} className="mt-6 flex flex-col gap-2 border-t border-line pt-6">
          <h2 className="text-lg font-medium text-ink">Add card</h2>
          <Input value={front} onChange={(e) => setFront(e.target.value)} placeholder="Front" required />
          <Input value={back} onChange={(e) => setBack(e.target.value)} placeholder="Back" required />
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Notes (optional)" />
          <Button type="submit" variant="primary" className="self-start">
            Add card
          </Button>
        </form>
      )}
    </div>
  )
}

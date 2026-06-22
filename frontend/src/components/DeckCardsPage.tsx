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
        <h1>Not found</h1>
        <button onClick={() => navigate('/courses')}>Back to courses</button>
      </div>
    )
  }
  if (!course || !cards) return <p style={{ padding: '1rem' }}>Loading…</p>

  async function onAdd(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await createCard(cId, dId, { front, back, notes: notes || null })
      setFront(''); setBack(''); setNotes('')
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
    <div style={{ maxWidth: 680 }}>
      <button onClick={() => navigate(`/courses/${cId}`)} style={{ marginBottom: '1rem' }}>← Back to course</button>
      <h1>Cards{course.visibility === 'PUBLIC' && <span style={{ fontSize: '0.8rem', color: '#888' }}> · public</span>}</h1>
      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}

      {cards.length === 0 ? (
        <p style={{ color: '#888' }}>No cards yet.</p>
      ) : (
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
          <button
            onClick={() => navigate(`/courses/${cId}/decks/${dId}/study`)}
            style={{ padding: '0.5rem 1.25rem' }}
          >
            ▶ Study this deck
          </button>
          <button
            onClick={() => navigate(`/courses/${cId}/decks/${dId}/review`)}
            style={{ padding: '0.5rem 1.25rem' }}
          >
            🔁 Review due
          </button>
        </div>
      )}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {cards.map((card) => (
          <li key={card.id} style={{ border: '1px solid #ccc', borderRadius: 6, padding: '0.75rem', marginBottom: '0.5rem' }}>
            {editing?.id === card.id ? (
              <form onSubmit={onSaveEdit} style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                <input value={editing.front} onChange={(e) => setEditing({ ...editing, front: e.target.value })} required placeholder="Front" />
                <input value={editing.back} onChange={(e) => setEditing({ ...editing, back: e.target.value })} required placeholder="Back" />
                <input value={editing.notes} onChange={(e) => setEditing({ ...editing, notes: e.target.value })} placeholder="Notes (optional)" />
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button type="submit">Save</button>
                  <button type="button" onClick={() => setEditing(null)}>Cancel</button>
                </div>
              </form>
            ) : (
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}>
                  <div><strong>{card.front}</strong></div>
                  <div style={{ color: '#aaa' }}>{card.back}</div>
                  {card.notes && <div style={{ fontSize: '0.8rem', color: '#888', marginTop: '0.25rem' }}>{card.notes}</div>}
                </div>
                {isOwner && (
                  <div style={{ display: 'flex', gap: '0.4rem' }}>
                    <button onClick={() => setEditing({ id: card.id, front: card.front, back: card.back, notes: card.notes ?? '' })}>Edit</button>
                    <button onClick={() => onDelete(card)} style={{ color: 'crimson' }}>Delete</button>
                  </div>
                )}
              </div>
            )}
          </li>
        ))}
      </ul>

      {isOwner && (
        <form onSubmit={onAdd} style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', marginTop: '1rem', borderTop: '1px solid #ccc', paddingTop: '1rem' }}>
          <h2 style={{ fontSize: '1rem' }}>Add card</h2>
          <input value={front} onChange={(e) => setFront(e.target.value)} placeholder="Front" required />
          <input value={back} onChange={(e) => setBack(e.target.value)} placeholder="Back" required />
          <input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Notes (optional)" />
          <button type="submit" style={{ alignSelf: 'flex-start' }}>Add card</button>
        </form>
      )}
    </div>
  )
}

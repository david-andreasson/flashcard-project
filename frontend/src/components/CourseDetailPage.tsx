import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import {
  createDeck,
  deleteCourse,
  deleteDeck,
  getCourse,
  listDecks,
  updateCourse,
  updateDeck,
  type Course,
  type Deck,
  type PagedResponse,
} from '../lib/courses'

export function CourseDetailPage() {
  const { id } = useParams()
  const courseId = Number(id)
  const { user } = useAuth()
  const navigate = useNavigate()

  const [course, setCourse] = useState<Course | null>(null)
  const [decks, setDecks] = useState<PagedResponse<Deck> | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [newDeckTitle, setNewDeckTitle] = useState('')
  const [editingTitle, setEditingTitle] = useState<string | null>(null)

  const isOwner = !!user && !!course && course.ownerId === user.id

  const loadDecks = useCallback(() => {
    listDecks(courseId, 0, 50).then(setDecks).catch((e) => setError(e.message))
  }, [courseId])

  useEffect(() => {
    getCourse(courseId)
      .then((c) => {
        setCourse(c)
        loadDecks()
      })
      .catch(() => setNotFound(true))
  }, [courseId, loadDecks])

  if (notFound) {
    return (
      <div>
        <h1>Course not found</h1>
        <button onClick={() => navigate('/courses')}>Back to courses</button>
      </div>
    )
  }
  if (!course) return <p style={{ padding: '1rem' }}>Loading…</p>

  async function onAddDeck(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await createDeck(courseId, newDeckTitle)
      setNewDeckTitle('')
      loadDecks()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not add deck')
    }
  }

  async function onSaveTitle(event: FormEvent) {
    event.preventDefault()
    if (editingTitle === null) return
    try {
      const updated = await updateCourse(courseId, { title: editingTitle })
      setCourse(updated)
      setEditingTitle(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save title')
    }
  }

  async function onDeleteCourse() {
    if (!window.confirm('Delete this course and all its decks?')) return
    try {
      await deleteCourse(courseId)
      navigate('/courses')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete course')
    }
  }

  async function onRenameDeck(deck: Deck) {
    const title = window.prompt('New deck title', deck.title)
    if (!title) return
    try {
      await updateDeck(courseId, deck.id, title)
      loadDecks()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not rename deck')
    }
  }

  async function onDeleteDeck(deck: Deck) {
    if (!window.confirm(`Delete deck "${deck.title}"?`)) return
    try {
      await deleteDeck(courseId, deck.id)
      loadDecks()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete deck')
    }
  }

  return (
    <div style={{ maxWidth: 640 }}>
      <button onClick={() => navigate('/courses')} style={{ marginBottom: '1rem' }}>← Courses</button>

      {editingTitle === null ? (
        <h1>
          {course.title}
          {course.visibility === 'PUBLIC' && (
            <span style={{ marginLeft: '0.5rem', fontSize: '0.8rem', color: '#888' }}>· public</span>
          )}
        </h1>
      ) : (
        <form onSubmit={onSaveTitle} style={{ display: 'flex', gap: '0.5rem' }}>
          <input value={editingTitle} onChange={(e) => setEditingTitle(e.target.value)} required
            style={{ flex: 1, padding: '0.4rem' }} />
          <button type="submit">Save</button>
          <button type="button" onClick={() => setEditingTitle(null)}>Cancel</button>
        </form>
      )}

      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}

      {isOwner && editingTitle === null && (
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
          <button onClick={() => setEditingTitle(course.title)}>Edit title</button>
          <button onClick={onDeleteCourse} style={{ color: 'crimson' }}>Delete course</button>
        </div>
      )}

      <h2>Decks</h2>
      {decks && decks.content.length === 0 && <p style={{ color: '#888' }}>No decks yet.</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {decks?.content.map((deck) => (
          <li key={deck.id} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', padding: '0.3rem 0' }}>
            <span style={{ flex: 1 }}>{deck.title}</span>
            {isOwner && <button onClick={() => onRenameDeck(deck)}>Rename</button>}
            {isOwner && <button onClick={() => onDeleteDeck(deck)} style={{ color: 'crimson' }}>Delete</button>}
          </li>
        ))}
      </ul>

      {isOwner && (
        <form onSubmit={onAddDeck} style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
          <input value={newDeckTitle} onChange={(e) => setNewDeckTitle(e.target.value)}
            placeholder="New deck title" required style={{ flex: 1, padding: '0.5rem' }} />
          <button type="submit">Add deck</button>
        </form>
      )}
    </div>
  )
}

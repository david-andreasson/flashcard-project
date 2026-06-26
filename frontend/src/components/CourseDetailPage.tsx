import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
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
import { Alert, Button, Input } from './ui'

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
        <h1 className="text-2xl font-medium text-ink">Course not found</h1>
        <Button onClick={() => navigate('/courses')} className="mt-4">
          Back to courses
        </Button>
      </div>
    )
  }
  if (!course) return <p className="text-muted">Loading…</p>

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
    <div>
      <Button onClick={() => navigate('/courses')} variant="ghost" className="mb-4 px-2 py-1">
        ← Courses
      </Button>

      {editingTitle === null ? (
        <h1 className="text-2xl font-medium text-ink">
          {course.title}
          {course.visibility === 'PUBLIC' && (
            <span className="ml-2 text-sm text-muted">· public</span>
          )}
        </h1>
      ) : (
        <form onSubmit={onSaveTitle} className="flex gap-2">
          <Input value={editingTitle} onChange={(e) => setEditingTitle(e.target.value)} required className="flex-1" />
          <Button type="submit" variant="primary">
            Save
          </Button>
          <Button type="button" onClick={() => setEditingTitle(null)}>
            Cancel
          </Button>
        </form>
      )}

      {error && (
        <div className="mt-3">
          <Alert tone="danger">{error}</Alert>
        </div>
      )}

      {isOwner && editingTitle === null && (
        <div className="mt-3 flex gap-2">
          <Button onClick={() => setEditingTitle(course.title)} className="px-3 py-1.5">
            Edit title
          </Button>
          <Button onClick={onDeleteCourse} className="px-3 py-1.5 text-danger">
            Delete course
          </Button>
        </div>
      )}

      <h2 className="mt-8 text-lg font-medium text-ink">Decks</h2>
      {decks && decks.content.length === 0 && <p className="mt-2 text-muted">No decks yet.</p>}
      <ul className="mt-2 list-none p-0">
        {decks?.content.map((deck) => (
          <li key={deck.id} className="flex flex-wrap items-center gap-2 border-b border-line py-2">
            <Link
              to={`/courses/${courseId}/decks/${deck.id}/study`}
              className="flex-1 font-medium no-underline hover:underline"
            >
              {deck.title}
            </Link>
            <Link
              to={`/courses/${courseId}/decks/${deck.id}`}
              className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink no-underline hover:border-accent hover:no-underline"
            >
              {isOwner ? 'Edit' : 'Cards'}
            </Link>
            {isOwner && (
              <Button onClick={() => onRenameDeck(deck)} className="px-3 py-1.5">
                Rename
              </Button>
            )}
            {isOwner && (
              <Button onClick={() => onDeleteDeck(deck)} className="px-3 py-1.5 text-danger">
                Delete
              </Button>
            )}
          </li>
        ))}
      </ul>

      {isOwner && (
        <form onSubmit={onAddDeck} className="mt-4 flex gap-2">
          <Input
            value={newDeckTitle}
            onChange={(e) => setNewDeckTitle(e.target.value)}
            placeholder="New deck title"
            required
            className="flex-1"
          />
          <Button type="submit" variant="primary">
            Add deck
          </Button>
        </form>
      )}
    </div>
  )
}

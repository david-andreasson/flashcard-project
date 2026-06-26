import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { createCourse, createDeck, listCourses, listDecks, type Course, type Deck } from '../lib/courses'
import { bulkCreateCards, extractPdf, generateCards } from '../lib/aiCards'
import { Alert, Button, Input, Select, Textarea } from './ui'

interface DraftRow {
  id: number
  front: string
  back: string
  include: boolean
}

interface Usage {
  inputTokens: number
  outputTokens: number
  model: string
}

// A save target selection: an existing id, nothing chosen (''), or the "create new" sentinel.
type Target = number | '' | 'new'

function parseTarget(value: string): Target {
  if (value === '') return ''
  if (value === 'new') return 'new'
  return Number(value)
}

export function AiGeneratePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const canUseAi = !!user && (user.plan === 'PREMIUM' || user.role === 'ADMIN')

  const [text, setText] = useState('')
  const [count, setCount] = useState('')
  const [drafts, setDrafts] = useState<DraftRow[]>([])
  const [usage, setUsage] = useState<Usage | null>(null)
  const [generating, setGenerating] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<{ count: number; courseId: number; deckId: number } | null>(null)
  const [pdfNotice, setPdfNotice] = useState<string | null>(null)
  const [extracting, setExtracting] = useState(false)

  const [courses, setCourses] = useState<Course[]>([])
  const [decks, setDecks] = useState<Deck[]>([])
  const [courseId, setCourseId] = useState<Target>('')
  const [deckId, setDeckId] = useState<Target>('')
  const [newCourseTitle, setNewCourseTitle] = useState('')
  const [newDeckTitle, setNewDeckTitle] = useState('')
  // Cache entities created during a save so a retry after a partial failure reuses them.
  const createdCourse = useRef<Course | null>(null)
  const createdDeck = useRef<Deck | null>(null)

  // Load the user's own courses (only owned courses are writable) once, if AI is available to them.
  useEffect(() => {
    if (!canUseAi) return
    listCourses('mine').then((p) => setCourses(p.content)).catch((e) => setError(e.message))
  }, [canUseAi])

  // Load decks whenever the chosen course changes.
  useEffect(() => {
    if (courseId === '') {
      setDecks([])
      setDeckId('')
      return
    }
    if (courseId === 'new') {
      // A brand-new course has no decks, so the deck must be new too.
      setDecks([])
      setDeckId('new')
      return
    }
    listDecks(courseId).then((p) => setDecks(p.content)).catch((e) => setError(e.message))
    setDeckId('')
  }, [courseId])

  // Any change to the chosen target invalidates a creation cached from a previous save attempt.
  useEffect(() => {
    createdCourse.current = null
    createdDeck.current = null
  }, [courseId, deckId, newCourseTitle, newDeckTitle])

  if (!canUseAi) {
    return (
      <div>
        <h1 className="text-2xl font-medium text-ink">Generate cards with AI</h1>
        <p role="alert" className="mt-3 text-danger">
          AI features require a PREMIUM plan. Your account does not have access.
        </p>
      </div>
    )
  }

  async function onPdf(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = '' // allow re-selecting the same file
    if (!file) return
    setError(null)
    setSaved(null)
    setPdfNotice(null)
    setExtracting(true)
    try {
      const res = await extractPdf(file)
      setText(res.text)
      setPdfNotice(
        `Extracted ${res.charCount} characters from ${res.pageCount} page${res.pageCount === 1 ? '' : 's'}` +
          (res.truncated ? ` (truncated to ${res.text.length} for generation).` : '.'),
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not read the PDF')
    } finally {
      setExtracting(false)
    }
  }

  async function onGenerate(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaved(null)
    setGenerating(true)
    try {
      const n = count.trim() === '' ? undefined : Number(count)
      const res = await generateCards(text, n)
      setDrafts(res.drafts.map((d, i) => ({ id: i, front: d.front, back: d.back, include: true })))
      setUsage({ inputTokens: res.inputTokens, outputTokens: res.outputTokens, model: res.model })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Generation failed')
      setDrafts([])
      setUsage(null)
    } finally {
      setGenerating(false)
    }
  }

  function patchDraft(id: number, patch: Partial<DraftRow>) {
    setDrafts((rows) => rows.map((r) => (r.id === id ? { ...r, ...patch } : r)))
  }

  function removeDraft(id: number) {
    setDrafts((rows) => rows.filter((r) => r.id !== id))
  }

  async function onSave() {
    const chosen = drafts.filter((d) => d.include && d.front.trim() && d.back.trim())
    if (chosen.length === 0) {
      setError('Select at least one draft with a front and back.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      // Resolve the target course — create it if "new", reusing a prior creation on retry.
      let cId: number
      if (courseId === 'new') {
        let course = createdCourse.current
        if (!course) {
          const created = await createCourse(newCourseTitle.trim())
          createdCourse.current = created
          setCourses((prev) => [...prev, created])
          course = created
        }
        cId = course.id
      } else if (typeof courseId === 'number') {
        cId = courseId
      } else {
        setError('Pick a course to save into.')
        return
      }

      // Resolve the target deck — a new course always needs a new deck.
      let dId: number
      if (courseId === 'new' || deckId === 'new') {
        let deck = createdDeck.current
        if (!deck) {
          const created = await createDeck(cId, newDeckTitle.trim())
          createdDeck.current = created
          setDecks((prev) => [...prev, created])
          deck = created
        }
        dId = deck.id
      } else if (typeof deckId === 'number') {
        dId = deckId
      } else {
        setError('Pick a deck to save into.')
        return
      }

      await bulkCreateCards(cId, dId, chosen.map((d) => ({ front: d.front, back: d.back })))
      setSaved({ count: chosen.length, courseId: cId, deckId: dId })
      setDrafts([])
      setUsage(null)
      setNewCourseTitle('')
      setNewDeckTitle('')
      createdCourse.current = null
      createdDeck.current = null
      // Keep a freshly created deck selected for the next batch when the course already existed;
      // for a brand-new course, return to a clean slate (the new course/deck are now in the lists).
      if (courseId === 'new') {
        setCourseId('')
        setDeckId('')
      } else if (deckId === 'new') {
        setDeckId(dId)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  const selectedCount = drafts.filter((d) => d.include).length
  const newCourseOk = newCourseTitle.trim().length > 0 && newCourseTitle.trim().length <= 200
  const newDeckOk = newDeckTitle.trim().length > 0 && newDeckTitle.trim().length <= 200
  const targetReady =
    courseId === 'new'
      ? newCourseOk && newDeckOk
      : typeof courseId === 'number'
        ? deckId === 'new'
          ? newDeckOk
          : typeof deckId === 'number'
        : false

  const fieldGroup = 'flex flex-col gap-1.5 min-w-[170px]'

  return (
    <div>
      <h1 className="text-2xl font-medium text-ink">Generate cards with AI</h1>
      {error && (
        <div className="mt-3">
          <Alert tone="danger">{error}</Alert>
        </div>
      )}
      {saved && (
        <div className="mt-3 flex items-center gap-3 text-sm text-success">
          Saved {saved.count} card{saved.count === 1 ? '' : 's'}.
          <Button onClick={() => navigate(`/courses/${saved.courseId}/decks/${saved.deckId}`)} className="px-3 py-1.5">
            Go to deck
          </Button>
        </div>
      )}

      <form onSubmit={onGenerate} className="mt-4 flex flex-col gap-3">
        <label htmlFor="source-text" className="text-sm text-muted">
          Paste text to turn into flashcards
        </label>
        <Textarea
          id="source-text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={8}
          required
          placeholder="Paste notes, a paragraph, definitions…"
        />
        <div className="flex flex-wrap items-center gap-2">
          <label htmlFor="pdf" className="text-sm text-muted">
            Or upload a PDF:
          </label>
          <input
            id="pdf"
            type="file"
            accept="application/pdf,.pdf"
            onChange={onPdf}
            disabled={extracting}
            className="text-sm text-muted file:mr-3 file:rounded-lg file:border file:border-line file:bg-surface file:px-3 file:py-1.5 file:text-sm file:text-ink"
          />
          {extracting && <span className="text-sm text-muted">Extracting…</span>}
        </div>
        {pdfNotice && <p className="text-xs text-muted">{pdfNotice}</p>}
        <div className="flex items-center gap-2">
          <label htmlFor="count" className="text-sm text-muted">
            Max cards (optional)
          </label>
          <input
            id="count"
            type="number"
            min={1}
            value={count}
            onChange={(e) => setCount(e.target.value)}
            className="w-20 rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
          />
          <Button type="submit" variant="primary" disabled={generating || text.trim() === ''}>
            {generating ? 'Generating…' : 'Generate'}
          </Button>
        </div>
      </form>

      {usage && (
        <p className="mt-3 text-xs text-muted">
          {drafts.length} draft{drafts.length === 1 ? '' : 's'} · model {usage.model} · ~
          {usage.inputTokens + usage.outputTokens} tokens (estimated)
        </p>
      )}

      {drafts.length > 0 && (
        <>
          <h2 className="mt-6 text-lg font-medium text-ink">Review drafts</h2>
          <ul className="mt-3 list-none p-0">
            {drafts.map((d) => (
              <li key={d.id} className="mb-2 rounded-xl border border-line bg-surface p-4">
                <div className="flex items-start gap-3">
                  <input
                    type="checkbox"
                    checked={d.include}
                    onChange={(e) => patchDraft(d.id, { include: e.target.checked })}
                    aria-label="Include this card"
                    className="mt-2 accent-[var(--color-accent)]"
                  />
                  <div className="flex flex-1 flex-col gap-2">
                    <Input value={d.front} onChange={(e) => patchDraft(d.id, { front: e.target.value })} placeholder="Front" />
                    <Input value={d.back} onChange={(e) => patchDraft(d.id, { back: e.target.value })} placeholder="Back" />
                  </div>
                  <Button type="button" onClick={() => removeDraft(d.id)} className="px-3 py-1.5 text-danger">
                    Remove
                  </Button>
                </div>
              </li>
            ))}
          </ul>

          <div className="mt-4 flex flex-wrap items-end gap-3 rounded-xl border border-line bg-surface p-4">
            <div className={fieldGroup}>
              <label htmlFor="course" className="text-sm text-muted">Save to course</label>
              <Select id="course" value={courseId} onChange={(e) => setCourseId(parseTarget(e.target.value))}>
                <option value="">Select a course…</option>
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>{c.title}</option>
                ))}
                <option value="new">+ New course…</option>
              </Select>
            </div>

            {courseId === 'new' && (
              <div className={fieldGroup}>
                <label htmlFor="new-course" className="text-sm text-muted">New course title</label>
                <Input id="new-course" value={newCourseTitle} onChange={(e) => setNewCourseTitle(e.target.value)} placeholder="New course title" maxLength={200} />
              </div>
            )}

            <div className={fieldGroup}>
              <label htmlFor="deck" className="text-sm text-muted">Deck</label>
              {courseId === 'new' ? (
                <Input
                  id="deck"
                  aria-label="New deck title"
                  value={newDeckTitle}
                  onChange={(e) => setNewDeckTitle(e.target.value)}
                  placeholder="New deck title"
                  maxLength={200}
                />
              ) : (
                <Select
                  id="deck"
                  value={deckId}
                  onChange={(e) => setDeckId(parseTarget(e.target.value))}
                  disabled={typeof courseId !== 'number'}
                >
                  <option value="">Select a deck…</option>
                  {decks.map((d) => (
                    <option key={d.id} value={d.id}>{d.title}</option>
                  ))}
                  <option value="new">+ New deck…</option>
                </Select>
              )}
            </div>

            {courseId !== 'new' && deckId === 'new' && (
              <div className={fieldGroup}>
                <label htmlFor="new-deck" className="text-sm text-muted">New deck title</label>
                <Input id="new-deck" value={newDeckTitle} onChange={(e) => setNewDeckTitle(e.target.value)} placeholder="New deck title" maxLength={200} />
              </div>
            )}

            <Button
              type="button"
              variant="primary"
              onClick={onSave}
              disabled={saving || selectedCount === 0 || !targetReady}
              className="ml-auto"
            >
              {saving ? 'Saving…' : `Save ${selectedCount} card${selectedCount === 1 ? '' : 's'}`}
            </Button>
          </div>
        </>
      )}
    </div>
  )
}

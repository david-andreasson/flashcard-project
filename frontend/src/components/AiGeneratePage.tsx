import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { listCourses, listDecks, type Course, type Deck } from '../lib/courses'
import { bulkCreateCards, extractPdf, generateCards } from '../lib/aiCards'

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
  const [courseId, setCourseId] = useState<number | ''>('')
  const [deckId, setDeckId] = useState<number | ''>('')

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
    listDecks(courseId).then((p) => setDecks(p.content)).catch((e) => setError(e.message))
    setDeckId('')
  }, [courseId])

  if (!canUseAi) {
    return (
      <div style={{ maxWidth: 680 }}>
        <h1>Generate cards with AI</h1>
        <p role="alert" style={{ color: 'crimson' }}>
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
    if (courseId === '' || deckId === '') {
      setError('Pick a course and deck to save into.')
      return
    }
    const chosen = drafts.filter((d) => d.include && d.front.trim() && d.back.trim())
    if (chosen.length === 0) {
      setError('Select at least one draft with a front and back.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await bulkCreateCards(courseId, deckId, chosen.map((d) => ({ front: d.front, back: d.back })))
      setSaved({ count: chosen.length, courseId, deckId })
      setDrafts([])
      setUsage(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  const selectedCount = drafts.filter((d) => d.include).length

  return (
    <div style={{ maxWidth: 680 }}>
      <h1>Generate cards with AI</h1>
      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}
      {saved && (
        <p style={{ color: 'green', display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          Saved {saved.count} card{saved.count === 1 ? '' : 's'}.
          <button onClick={() => navigate(`/courses/${saved.courseId}/decks/${saved.deckId}`)}>
            Go to deck
          </button>
        </p>
      )}

      <form onSubmit={onGenerate} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
        <label htmlFor="source-text">Paste text to turn into flashcards</label>
        <textarea
          id="source-text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={8}
          required
          placeholder="Paste notes, a paragraph, definitions…"
        />
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <label htmlFor="pdf">Or upload a PDF:</label>
          <input id="pdf" type="file" accept="application/pdf,.pdf" onChange={onPdf} disabled={extracting} />
          {extracting && <span style={{ color: '#888' }}>Extracting…</span>}
        </div>
        {pdfNotice && <p style={{ fontSize: '0.8rem', color: '#888', margin: 0 }}>{pdfNotice}</p>}
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <label htmlFor="count">Max cards (optional)</label>
          <input
            id="count"
            type="number"
            min={1}
            value={count}
            onChange={(e) => setCount(e.target.value)}
            style={{ width: 80 }}
          />
          <button type="submit" disabled={generating || text.trim() === ''}>
            {generating ? 'Generating…' : 'Generate'}
          </button>
        </div>
      </form>

      {usage && (
        <p style={{ fontSize: '0.8rem', color: '#888', marginTop: '0.75rem' }}>
          {drafts.length} draft{drafts.length === 1 ? '' : 's'} · model {usage.model} · ~
          {usage.inputTokens + usage.outputTokens} tokens (estimated)
        </p>
      )}

      {drafts.length > 0 && (
        <>
          <h2 style={{ fontSize: '1rem', marginTop: '1rem' }}>Review drafts</h2>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {drafts.map((d) => (
              <li
                key={d.id}
                style={{ border: '1px solid #ccc', borderRadius: 6, padding: '0.75rem', marginBottom: '0.5rem' }}
              >
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-start' }}>
                  <input
                    type="checkbox"
                    checked={d.include}
                    onChange={(e) => patchDraft(d.id, { include: e.target.checked })}
                    aria-label="Include this card"
                    style={{ marginTop: '0.4rem' }}
                  />
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                    <input value={d.front} onChange={(e) => patchDraft(d.id, { front: e.target.value })} placeholder="Front" />
                    <input value={d.back} onChange={(e) => patchDraft(d.id, { back: e.target.value })} placeholder="Back" />
                  </div>
                  <button type="button" onClick={() => removeDraft(d.id)} style={{ color: 'crimson' }}>
                    Remove
                  </button>
                </div>
              </li>
            ))}
          </ul>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center', borderTop: '1px solid #ccc', paddingTop: '1rem' }}>
            <label htmlFor="course">Save to course</label>
            <select
              id="course"
              value={courseId}
              onChange={(e) => setCourseId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <option value="">Select a course…</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>{c.title}</option>
              ))}
            </select>
            <label htmlFor="deck">deck</label>
            <select
              id="deck"
              value={deckId}
              onChange={(e) => setDeckId(e.target.value === '' ? '' : Number(e.target.value))}
              disabled={courseId === ''}
            >
              <option value="">Select a deck…</option>
              {decks.map((d) => (
                <option key={d.id} value={d.id}>{d.title}</option>
              ))}
            </select>
            <button type="button" onClick={onSave} disabled={saving || selectedCount === 0}>
              {saving ? 'Saving…' : `Save ${selectedCount} card${selectedCount === 1 ? '' : 's'}`}
            </button>
          </div>
        </>
      )}
    </div>
  )
}

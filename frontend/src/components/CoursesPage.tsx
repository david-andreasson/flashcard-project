import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { createCourse, listCourses, type Course, type PagedResponse } from '../lib/courses'
import { Alert, Button, Input } from './ui'

const PAGE_SIZE = 10

function CourseCard({ course }: { course: Course }) {
  return (
    <Link
      to={`/courses/${course.id}`}
      className="block rounded-xl border border-line bg-surface p-4 no-underline shadow-[var(--shadow)] transition hover:-translate-y-0.5 hover:border-accent hover:no-underline"
    >
      <div className="flex items-start justify-between gap-2">
        <span className="font-medium text-ink">{course.title}</span>
        {course.visibility === 'PUBLIC' && (
          <span className="shrink-0 rounded-full border border-line px-2 py-0.5 text-xs text-muted">
            public
          </span>
        )}
      </div>
      <div className="mt-2 text-sm text-muted">
        {course.deckCount} deck{course.deckCount === 1 ? '' : 's'} · {course.cardCount} card
        {course.cardCount === 1 ? '' : 's'}
      </div>
    </Link>
  )
}

export function CoursesPage() {
  const [mine, setMine] = useState<PagedResponse<Course> | null>(null)
  const [pub, setPub] = useState<PagedResponse<Course> | null>(null)
  const [minePage, setMinePage] = useState(0)
  const [newTitle, setNewTitle] = useState('')
  const [error, setError] = useState<string | null>(null)

  const loadMine = useCallback((page: number) => {
    listCourses('mine', page, PAGE_SIZE).then(setMine).catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    loadMine(minePage)
  }, [minePage, loadMine])

  useEffect(() => {
    listCourses('public', 0, 20).then(setPub).catch((e) => setError(e.message))
  }, [])

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await createCourse(newTitle)
      setNewTitle('')
      if (minePage === 0) loadMine(0)
      else setMinePage(0)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create course')
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-medium text-ink">Courses</h1>
      {error && (
        <div className="mt-3">
          <Alert tone="danger">{error}</Alert>
        </div>
      )}

      <form onSubmit={onCreate} className="mt-4 flex gap-2">
        <Input
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
          placeholder="New course title"
          required
          className="flex-1"
        />
        <Button type="submit" variant="primary">
          Add course
        </Button>
      </form>

      <h2 className="mt-8 text-lg font-medium text-ink">My courses</h2>
      {mine && mine.content.length === 0 && (
        <p className="mt-2 text-muted">You don't have any courses yet. Create one above.</p>
      )}
      {mine && mine.content.length > 0 && (
        <>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            {mine.content.map((c) => (
              <CourseCard key={c.id} course={c} />
            ))}
          </div>
          {mine.totalPages > 1 && (
            <div className="mt-4 flex items-center gap-3">
              <Button disabled={minePage === 0} onClick={() => setMinePage((p) => p - 1)} className="px-3 py-1.5">
                Prev
              </Button>
              <span className="text-sm text-muted">
                Page {mine.page + 1} of {mine.totalPages}
              </span>
              <Button
                disabled={minePage >= mine.totalPages - 1}
                onClick={() => setMinePage((p) => p + 1)}
                className="px-3 py-1.5"
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}

      <h2 className="mt-8 text-lg font-medium text-ink">Public courses</h2>
      {pub && pub.content.length === 0 && <p className="mt-2 text-muted">No public courses yet.</p>}
      {pub && pub.content.length > 0 && (
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          {pub.content.map((c) => (
            <CourseCard key={c.id} course={c} />
          ))}
        </div>
      )}
    </div>
  )
}

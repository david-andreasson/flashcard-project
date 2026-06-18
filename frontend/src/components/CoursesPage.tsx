import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { createCourse, listCourses, type Course, type PagedResponse } from '../lib/courses'

const PAGE_SIZE = 10

function CourseLink({ course }: { course: Course }) {
  return (
    <li style={{ padding: '0.4rem 0' }}>
      <Link to={`/courses/${course.id}`}>{course.title}</Link>
      {course.visibility === 'PUBLIC' && (
        <span style={{ marginLeft: '0.5rem', fontSize: '0.75rem', color: '#888' }}>· public</span>
      )}
    </li>
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
    <div style={{ maxWidth: 640 }}>
      <h1>Courses</h1>
      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}

      <form onSubmit={onCreate} style={{ display: 'flex', gap: '0.5rem', margin: '1rem 0' }}>
        <input
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
          placeholder="New course title"
          required
          style={{ flex: 1, padding: '0.5rem' }}
        />
        <button type="submit" style={{ padding: '0.5rem 1rem' }}>Add course</button>
      </form>

      <h2>My courses</h2>
      {mine && mine.content.length === 0 && (
        <p style={{ color: '#888' }}>You don't have any courses yet. Create one above.</p>
      )}
      {mine && mine.content.length > 0 && (
        <>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {mine.content.map((c) => <CourseLink key={c.id} course={c} />)}
          </ul>
          {mine.totalPages > 1 && (
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <button disabled={minePage === 0} onClick={() => setMinePage((p) => p - 1)}>Prev</button>
              <span style={{ fontSize: '0.85rem' }}>Page {mine.page + 1} of {mine.totalPages}</span>
              <button disabled={minePage >= mine.totalPages - 1} onClick={() => setMinePage((p) => p + 1)}>Next</button>
            </div>
          )}
        </>
      )}

      <h2 style={{ marginTop: '2rem' }}>Public courses</h2>
      {pub && pub.content.length === 0 && <p style={{ color: '#888' }}>No public courses yet.</p>}
      {pub && (
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {pub.content.map((c) => <CourseLink key={c.id} course={c} />)}
        </ul>
      )}
    </div>
  )
}

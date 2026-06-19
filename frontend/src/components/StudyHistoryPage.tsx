import { useEffect, useState } from 'react'
import type { PagedResponse } from '../lib/courses'
import { listMySessions, type StudySession } from '../lib/study'

function formatDate(iso: string): string {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString()
}

export function StudyHistoryPage() {
  const [data, setData] = useState<PagedResponse<StudySession> | null>(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listMySessions(page, 20).then(setData).catch((e) => setError(e.message))
  }, [page])

  return (
    <div style={{ maxWidth: 640 }}>
      <h1>Study history</h1>
      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}

      {data && data.content.length === 0 && (
        <p style={{ color: '#888' }}>No study sessions yet. Study a deck to see it here.</p>
      )}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {data?.content.map((s) => (
          <li
            key={s.id}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              borderBottom: '1px solid #eee',
              padding: '0.6rem 0',
            }}
          >
            <span>
              <strong>{s.deckTitle}</strong>{' '}
              <span style={{ color: '#888' }}>· {formatDate(s.finishedAt)}</span>
            </span>
            <span>
              {s.correctCount}/{s.totalCards}
            </span>
          </li>
        ))}
      </ul>

      {data && data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginTop: '0.5rem' }}>
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Prev</button>
          <span style={{ fontSize: '0.85rem' }}>Page {data.page + 1} of {data.totalPages}</span>
          <button disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
        </div>
      )}
    </div>
  )
}

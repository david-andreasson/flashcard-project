import { useEffect, useState } from 'react'
import type { PagedResponse } from '../lib/courses'
import { listMySessions, type StudySession } from '../lib/study'
import { Alert, Button } from './ui'

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
    <div>
      <h1 className="text-2xl font-medium text-ink">Study history</h1>
      {error && (
        <div className="mt-3">
          <Alert tone="danger">{error}</Alert>
        </div>
      )}

      {data && data.content.length === 0 && (
        <p className="mt-3 text-muted">No study sessions yet. Study a deck to see it here.</p>
      )}

      <ul className="mt-4 list-none p-0">
        {data?.content.map((s) => (
          <li
            key={s.id}
            className="flex items-center justify-between gap-3 border-b border-line py-3 text-sm"
          >
            <span>
              <span className="font-medium text-ink">{s.deckTitle}</span>{' '}
              <span className="text-muted">· {formatDate(s.finishedAt)}</span>
            </span>
            <span className="text-ink">
              {s.correctCount}/{s.totalCards}
            </span>
          </li>
        ))}
      </ul>

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center gap-3">
          <Button onClick={() => setPage((p) => p - 1)} disabled={page === 0} className="px-3 py-1.5">
            Prev
          </Button>
          <span className="text-sm text-muted">
            Page {data.page + 1} of {data.totalPages}
          </span>
          <Button
            onClick={() => setPage((p) => p + 1)}
            disabled={page >= data.totalPages - 1}
            className="px-3 py-1.5"
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}

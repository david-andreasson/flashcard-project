import { useEffect, useState } from 'react'
import { getProgress, type Progress } from '../lib/study'
import { Alert } from './ui'

export function ProgressPage() {
  const [progress, setProgress] = useState<Progress | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getProgress()
      .then(setProgress)
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load progress'))
  }, [])

  if (error) return <Alert tone="danger">{error}</Alert>
  if (!progress) return <p className="text-muted">Loading…</p>

  const stats = [
    { label: 'Due now', value: progress.dueNow },
    { label: 'In review', value: progress.inReview },
    { label: 'Reviewed today', value: progress.reviewedToday },
  ]

  return (
    <div>
      <h1 className="text-2xl font-medium text-ink">Progress</h1>
      <p className="mt-1 text-muted">Your spaced-repetition status across all decks.</p>
      <div className="mt-6 grid gap-3 sm:grid-cols-3">
        {stats.map((s) => (
          <div key={s.label} className="rounded-xl border border-line bg-surface p-5 text-center">
            <div className="text-3xl font-medium text-ink">{s.value}</div>
            <div className="mt-1 text-sm text-muted">{s.label}</div>
          </div>
        ))}
      </div>
    </div>
  )
}

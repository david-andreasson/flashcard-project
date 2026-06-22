import { useEffect, useState } from 'react'
import { getProgress, type Progress } from '../lib/study'

export function ProgressPage() {
  const [progress, setProgress] = useState<Progress | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getProgress()
      .then(setProgress)
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load progress'))
  }, [])

  if (error) return <p role="alert" style={{ color: 'crimson', padding: '1rem' }}>{error}</p>
  if (!progress) return <p style={{ padding: '1rem' }}>Loading…</p>

  const stats = [
    { label: 'Due now', value: progress.dueNow },
    { label: 'In review', value: progress.inReview },
    { label: 'Reviewed today', value: progress.reviewedToday },
  ]

  return (
    <div style={{ maxWidth: 640 }}>
      <h1>Progress</h1>
      <p style={{ color: '#888' }}>Your spaced-repetition status across all decks.</p>
      <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginTop: '1rem' }}>
        {stats.map((s) => (
          <div
            key={s.label}
            style={{ border: '1px solid #ccc', borderRadius: 10, padding: '1.25rem 2rem', textAlign: 'center', minWidth: 130 }}
          >
            <div style={{ fontSize: '2rem', fontWeight: 700 }}>{s.value}</div>
            <div style={{ color: '#888' }}>{s.label}</div>
          </div>
        ))}
      </div>
    </div>
  )
}

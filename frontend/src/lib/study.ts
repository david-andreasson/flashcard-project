import { apiFetch, errorMessage } from './api'
import type { PagedResponse } from './courses'

export interface StudySession {
  id: number
  deckId: number
  deckTitle: string
  totalCards: number
  correctCount: number
  finishedAt: string
}

async function asJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(await errorMessage(res))
  }
  return (await res.json()) as T
}

export function recordSession(input: { deckId: number; totalCards: number; correctCount: number }) {
  return apiFetch('/study-sessions', { method: 'POST', body: JSON.stringify(input) }).then(asJson<StudySession>)
}

export function listMySessions(page = 0, size = 20) {
  return apiFetch(`/study-sessions?page=${page}&size=${size}`).then(asJson<PagedResponse<StudySession>>)
}

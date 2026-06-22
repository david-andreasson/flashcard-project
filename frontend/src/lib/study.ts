import { apiFetch, errorMessage } from './api'
import type { Card, PagedResponse } from './courses'

export type Grade = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'

export interface ReviewResult {
  intervalDays: number
  dueAt: string
  repetitions: number
  easeFactor: number
}

export interface Progress {
  dueNow: number
  inReview: number
  reviewedToday: number
}

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

// ── Spaced repetition (change 08) ──

/** Cards due (or new) in a deck for the current user, in review order. */
export function getDueCards(courseId: number, deckId: number) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}/due`).then(asJson<Card[]>)
}

/** Grade a card; the backend applies SM-2 and returns the new schedule. */
export function reviewCard(courseId: number, deckId: number, cardId: number, grade: Grade) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}/cards/${cardId}/review`, {
    method: 'POST',
    body: JSON.stringify({ grade }),
  }).then(asJson<ReviewResult>)
}

/** The current user's spaced-repetition progress counts. */
export function getProgress() {
  return apiFetch('/study/progress').then(asJson<Progress>)
}

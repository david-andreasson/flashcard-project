import { apiFetch, errorMessage } from './api'

export type Visibility = 'PUBLIC' | 'PRIVATE'

export interface Course {
  id: number
  ownerId: number
  title: string
  visibility: Visibility
  createdAt: string
}

export interface Deck {
  id: number
  courseId: number
  title: string
  createdAt: string
}

export interface Card {
  id: number
  deckId: number
  front: string
  back: string
  notes: string | null
  createdAt: string
}

export interface CardInput {
  front: string
  back: string
  notes?: string | null
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

async function asJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(await errorMessage(res))
  }
  return (await res.json()) as T
}

async function expectOk(res: Response): Promise<void> {
  if (!res.ok) {
    throw new Error(await errorMessage(res))
  }
}

// ── Courses ──
export function listCourses(scope: 'mine' | 'public', page = 0, size = 20) {
  return apiFetch(`/courses?scope=${scope}&page=${page}&size=${size}`).then(asJson<PagedResponse<Course>>)
}

export function getCourse(id: number) {
  return apiFetch(`/courses/${id}`).then(asJson<Course>)
}

export function createCourse(title: string) {
  return apiFetch('/courses', { method: 'POST', body: JSON.stringify({ title }) }).then(asJson<Course>)
}

export function updateCourse(id: number, body: { title?: string; visibility?: Visibility }) {
  return apiFetch(`/courses/${id}`, { method: 'PUT', body: JSON.stringify(body) }).then(asJson<Course>)
}

export function deleteCourse(id: number) {
  return apiFetch(`/courses/${id}`, { method: 'DELETE' }).then(expectOk)
}

// ── Decks ──
export function listDecks(courseId: number, page = 0, size = 20) {
  return apiFetch(`/courses/${courseId}/decks?page=${page}&size=${size}`).then(asJson<PagedResponse<Deck>>)
}

export function createDeck(courseId: number, title: string) {
  return apiFetch(`/courses/${courseId}/decks`, { method: 'POST', body: JSON.stringify({ title }) }).then(
    asJson<Deck>,
  )
}

export function updateDeck(courseId: number, deckId: number, title: string) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}`, {
    method: 'PUT',
    body: JSON.stringify({ title }),
  }).then(asJson<Deck>)
}

export function deleteDeck(courseId: number, deckId: number) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}`, { method: 'DELETE' }).then(expectOk)
}

// ── Cards ──
export function listCards(courseId: number, deckId: number, page = 0, size = 100) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}/cards?page=${page}&size=${size}`).then(
    asJson<PagedResponse<Card>>,
  )
}

export function createCard(courseId: number, deckId: number, input: CardInput) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}/cards`, {
    method: 'POST',
    body: JSON.stringify(input),
  }).then(asJson<Card>)
}

export function updateCard(courseId: number, deckId: number, cardId: number, input: CardInput) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}/cards/${cardId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  }).then(asJson<Card>)
}

export function deleteCard(courseId: number, deckId: number, cardId: number) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}/cards/${cardId}`, { method: 'DELETE' }).then(expectOk)
}

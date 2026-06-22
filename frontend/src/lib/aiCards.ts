import { apiFetch, errorMessage } from './api'
import type { Card, CardInput } from './courses'

export interface CardDraft {
  front: string
  back: string
}

export interface GenerateCardsResponse {
  drafts: CardDraft[]
  inputTokens: number
  outputTokens: number
  model: string
}

async function asJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(await errorMessage(res))
  }
  return (await res.json()) as T
}

/** Generate flashcard drafts from pasted text. Requires a PREMIUM plan or ADMIN role (enforced server-side). */
export function generateCards(text: string, count?: number) {
  const body = count != null ? { text, count } : { text }
  return apiFetch('/ai/cards/generate', { method: 'POST', body: JSON.stringify(body) }).then(
    asJson<GenerateCardsResponse>,
  )
}

/** Save reviewed drafts as real cards in one request. Plain card creation — no AI cost. */
export function bulkCreateCards(courseId: number, deckId: number, cards: CardInput[]) {
  return apiFetch(`/courses/${courseId}/decks/${deckId}/cards/bulk`, {
    method: 'POST',
    body: JSON.stringify({ cards }),
  }).then(asJson<Card[]>)
}

export interface ExtractedPdf {
  text: string
  pageCount: number
  charCount: number
  truncated: boolean
}

/** Upload a PDF and get its extracted text (capped to the AI input limit). No AI cost. */
export function extractPdf(file: File) {
  const form = new FormData()
  form.append('file', file)
  return apiFetch('/ai/cards/extract-pdf', { method: 'POST', body: form }).then(asJson<ExtractedPdf>)
}

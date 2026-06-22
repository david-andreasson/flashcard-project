# Flashcard App — Architecture

Key decisions made before implementation began. Changing these later would require significant rework.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.4.x, Spring Security, Spring Data JPA |
| Frontend | React 18, TypeScript 5 (strict), Vite 6, React Router v6 |
| Database | PostgreSQL 16 (Docker Compose locally on port 5433, RDS in production) |
| Migrations | Flyway — `ddl-auto=validate`, never auto-DDL |
| Build | Maven Wrapper (`./mvnw`), npm |
| AI | Pluggable `AiProvider` interface; first real provider: 1min.ai (model aggregator, Claude Haiku by default) |

---

## Repository layout

```
flashcard-project/
├── backend/        Spring Boot Maven project (package: com.flashcard)
├── frontend/       Vite + React + TypeScript
├── docs/           This file and ROADMAP.md
├── openspec/       Planning artifacts and main specs
└── docker-compose.yml
```

---

## Security

- **All endpoints protected by default** except `GET /actuator/health`
- **JWT in HttpOnly cookies** — never localStorage (XSS protection)
- **CSRF header** required on state-changing requests
- **User roles:** `USER` | `ADMIN`
- **User plans:** `FREE` | `PREMIUM`
- **AI features** require `PREMIUM` plan or `ADMIN` role

---

## API URL convention

Spring Boot runs with `server.servlet.context-path=/api`.  
Every controller route is implicitly under `/api` — no prefix needed in annotations.

| Environment | How `/api/*` reaches Spring Boot |
|---|---|
| Local dev | Vite proxy: browser → `localhost:5173/api/*` → `localhost:8080/api/*` |
| Production | Reverse proxy / ALB routes `/api/*` to the backend container |

This means `@GetMapping("/courses")` is reached at `/api/courses` everywhere.

---

## Data model overview

```
User
  id, email, passwordHash
  role: USER | ADMIN
  plan: FREE | PREMIUM

Course          ← "Biology 101"
  id, ownerId, title
  visibility: PUBLIC | PRIVATE        ← PUBLIC = readable by everyone (e.g. ~10 official courses)

Deck            ← "Chapter 3 vocab"
  id, courseId, title                 ← inherits its course's visibility

Card            ← shared CONTENT only (one row, studied by many)
  id, deckId
  front, back, notes

UserCardState   ← per-user SM-2 state (change 08); one row per (user, card), created lazily on first review
  id, userId, cardId
  easeFactor       (SM-2, default 2.5, floored at 1.3)
  intervalDays     (SM-2, default 0)
  repetitions      (SM-2, default 0)
  dueAt, lastReviewedAt
  UNIQUE (userId, cardId)

AiUsageLog      ← cost tracking
  id, userId, featureKey
  inputTokens, outputTokens, estimatedCostUsd
```

**Why study state is separate from the card:** courses can be PUBLIC and studied by many
users at once. SM-2 progress (ease/interval/due) is therefore *per user*, so it cannot live
on the shared `Card` row — it lives in `UserCardState`, keyed by `(userId, cardId)`. The
card holds content; each user has their own schedule for it.

**Ownership rule (established change 03):**
- READ a course/deck if `ownerId = me` **OR** `visibility = PUBLIC`
- WRITE (edit/delete) only if `ownerId = me` (or ADMIN)
- Enforced via ownership-scoped repository queries; not-owned → 404 (does not reveal existence)

---

## AI provider abstraction (change 06, first real provider in 07)

AI features use a pluggable interface so the provider can be swapped without touching quota or logging code.

```
interface AiProvider {
    AiResponse complete(AiRequest request)
}

AiRequest  { systemPrompt, userMessage, maxTokens, featureKey }
AiResponse { content, inputTokens, outputTokens, modelId }
```

Active provider selected via `ai.provider=mock|1min` (`mock` is the default — deterministic,
free, no key). Token counts from `AiResponse` flow into `AiUsageLog` for quota enforcement.
Providers whose backend does not report usage (1min.ai) estimate tokens via a shared
`TokenEstimator` (~4 chars/token), so quota and logging are unchanged; the logged USD cost is
then an estimate (1min.ai meters in credits, the token quota is the real spend guardrail).

**1min.ai provider (change 07):** `OneMinAiProvider` (active when `ai.provider=1min`) calls
`POST {base-url}/api/chat-with-ai` with header `API-KEY`, body
`{type:"UNIFY_CHAT_WITH_AI", model, promptObject:{prompt}}`, and reads the text at
`aiRecord.aiRecordDetail.resultObject`. Config is provider-scoped under `ai.onemin.*` (base URL,
API key from `ONEMIN_API_KEY`, model). An upstream failure or unusable response surfaces as HTTP 502.

---

## AI card generation (change 07)

Paste text → drafts → review → save, with the cost guard wrapping only generation:

1. `POST /api/ai/cards/generate` runs the guarded `AiService` pipeline (kill-switch → plan gate →
   input limit → quota → provider → usage log), then asks the model for a strict JSON array of
   `{front, back}` and parses it. It persists nothing.
2. The client reviews/edits/selects drafts and picks a target deck they own.
3. `POST /api/courses/{courseId}/decks/{deckId}/cards/bulk` saves the chosen drafts as cards —
   plain ownership-checked card creation, **no AI cost**.

Structured output is provider-neutral: the prompt requests JSON and the service parses it, rather
than using any provider-specific structured-output API — so it works for 1min.ai and any future provider.

---

## Spaced repetition (change 08)

Per-user SM-2 scheduling. Grading a card Again / Hard / Good / Easy maps to SM-2 quality 1 / 3 / 4 / 5
and updates the user's `UserCardState` (created lazily on first review, so a shared card schedules
independently per user). A failing grade resets the card; passes grow the interval 1 → 6 →
`round(interval × ease)`, with ease floored at 1.3. Endpoints:

- `POST /api/courses/{c}/decks/{d}/cards/{id}/review` — grade a card, returns the next due date
- `GET /api/courses/{c}/decks/{d}/due` — the user's due + new cards for a readable deck
- `GET /api/study/progress` — counts (due now, in review, reviewed today)

The study frontend adds a due-review mode (four-button grading) and a progress view. Scheduling is
day-grained and computed against a UTC "today" boundary.

---

## Patterns established early

| Pattern | Established in | Why it must be early |
|---|---|---|
| Ownership checks on all data endpoints (read = owned-or-public, write = owned) | change 03 | Retrofitting is error-prone and creates security gaps |
| Course `visibility` (PUBLIC/PRIVATE) | change 03 | Public/shared courses change every ownership query; adding later is a wide refactor |
| Pagination on all list endpoints | change 03 | Adding later is a breaking API change |
| Study state per-user in `UserCardState`, not on `Card` | change 04 | Shared/public decks need per-user SM-2 progress; moving it off the card later = migration + backfill |
| AI quota infrastructure before any AI feature | change 06 | No AI feature ships without cost protection in place |

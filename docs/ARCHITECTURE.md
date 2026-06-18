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
| AI | Pluggable `AiProvider` interface; first implementation: Anthropic Claude API |

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

UserCardState   ← per-user study progress (change 04); one row per (user, card)
  id, userId, cardId
  easeFactor    (SM-2, default 2.5)
  intervalDays  (SM-2, default 1)
  dueAt         (SM-2, nullable)
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

## AI provider abstraction (change 06)

AI features use a pluggable interface so the provider can be swapped without touching quota or logging code.

```
interface AiProvider {
    AiResponse complete(AiRequest request)
}

AiRequest  { systemPrompt, userMessage, maxTokens }
AiResponse { content, inputTokens, outputTokens, modelId }
```

Active provider selected via `ai.provider=anthropic|openai` in application config.  
Token counts from `AiResponse` flow directly into `AiUsageLog` for quota enforcement.

---

## Patterns established early

| Pattern | Established in | Why it must be early |
|---|---|---|
| Ownership checks on all data endpoints (read = owned-or-public, write = owned) | change 03 | Retrofitting is error-prone and creates security gaps |
| Course `visibility` (PUBLIC/PRIVATE) | change 03 | Public/shared courses change every ownership query; adding later is a wide refactor |
| Pagination on all list endpoints | change 03 | Adding later is a breaking API change |
| Study state per-user in `UserCardState`, not on `Card` | change 04 | Shared/public decks need per-user SM-2 progress; moving it off the card later = migration + backfill |
| AI quota infrastructure before any AI feature | change 06 | No AI feature ships without cost protection in place |

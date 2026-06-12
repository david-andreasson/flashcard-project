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
  id, userId (owner), title

Deck            ← "Chapter 3 vocab"
  id, courseId, title

Card
  id, deckId
  front, back, notes
  easyFactor    (SM-2, default 2.5)   ← added in change 04
  intervalDays  (SM-2, default 1)     ← added in change 04
  dueAt         (SM-2, nullable)      ← added in change 04

AiUsageLog      ← cost tracking
  id, userId, featureKey
  inputTokens, outputTokens, estimatedCostUsd
```

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
| Ownership checks on all data endpoints | change 03 | Retrofitting is error-prone and creates security gaps |
| Pagination on all list endpoints | change 03 | Adding later is a breaking API change |
| SM-2 fields on Card entity | change 04 | Schema migration + data backfill if added later |
| AI quota infrastructure before any AI feature | change 06 | No AI feature ships without cost protection in place |

# Flashcard App — Development Roadmap

10 incremental changes, each implemented as an OpenSpec change.
Completed changes are archived under `openspec/changes/archive/`.

---

## Changes

| # | Name | Status | What it covers |
|---|---|---|---|
| 01 | `project-foundation` | ✅ Done | Monorepo scaffold, Docker Compose + PostgreSQL, Spring Boot skeleton, React + TypeScript shell, Vite dev proxy, health check |
| 02 | `auth` | ✅ Done | JWT in HttpOnly cookies + CSRF header, User entity (role/plan), full Spring Security config, login/register UI |
| 03 | `course-deck-mgmt` | ✅ Done | Course + Deck CRUD, `visibility` PUBLIC/PRIVATE (public read-by-all, write owner-only), ownership-check pattern, paginated list endpoints, frontend list/detail views |
| 04 | `flashcard-crud` | ✅ Done | Card entity (shared content: front / back / optional notes, plain text); nested `/cards` CRUD + card editor UI; **seed the public courses with sample decks + cards** (JSON seed file) so they have real, visible content. (`UserCardState` moved to change 08.) |
| 05 | `study-mode-basic` | ✅ Done | Study session API, random/round-robin card queue, session tracking, flip-card UI |
| 06 | `ai-quota-infra` | ✅ Done | AiProvider interface (pluggable), AiUsageLog table, plan-gating middleware, monthly token quotas — must precede all AI features |
| 07 | `ai-card-generation` | ✅ Done | Paste text → guarded AI pipeline → `AiProvider` (first real provider: 1min.ai) → `{front, back}` card drafts; client-side review then bulk-save into a deck; requires PREMIUM or ADMIN |
| 08 | `spaced-repetition` | Planned | Create the per-user `UserCardState` table (easeFactor, intervalDays, dueAt); SM-2 algorithm writing to it (lazy: a row appears on first review); due-date study queue, progress dashboard |
| 09 | `pdf-import` | Planned | PDF upload + text extraction (Apache PDFBox), feeds AI card generation, file size limits, S3 storage |
| 10 | `aws-deployment` | Planned | Dockerfile, S3 + CloudFront for frontend, RDS PostgreSQL, ECS Fargate, GitHub Actions CI/CD |

---

## Key dependencies

```
01 (foundation)
 └─ 02 (auth)
     └─ 03 (courses + decks)
         └─ 04 (flashcards)         ← SM-2 fields locked in here
             ├─ 05 (study mode)
             └─ 06 (AI quota infra) ← must come before any AI feature
                 ├─ 07 (AI card gen)
                 │   └─ 09 (PDF import)
                 └─ 08 (spaced repetition) ← uses SM-2 fields from 04
                         └─ 10 (AWS deploy)
```

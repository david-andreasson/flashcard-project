# Flashcard App — Development Roadmap

Incremental changes, each implemented as an OpenSpec change. Completed changes are archived under
`openspec/changes/archive/`. The original plan was 10 changes; it has since grown to 12, and the AWS
deployment is deferred.

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
| 08 | `spaced-repetition` | ✅ Done | Per-user `UserCardState` table (easeFactor, intervalDays, repetitions, dueAt), created lazily on first review; SM-2 from Again/Hard/Good/Easy grades; due-card queue + per-card review endpoint; progress summary; due-review mode + progress dashboard in the UI |
| 09 | `pdf-import` | ✅ Done | PDF upload + Apache PDFBox text extraction (`POST /ai/cards/extract-pdf`), capped to the AI input limit (raised to 24000) and fed to the existing generate → review → save flow; extract-and-discard (S3 deferred to the AWS deploy change) |
| 10 | `inline-deck-course-creation` | ✅ Done | Create a course and/or deck inline on the AI generate screen's save step (frontend-only, reuses existing endpoints), so drafts can be saved without leaving the page |
| 11 | `frontend-facelift` | ✅ Done | Tailwind v4 design system; five calm, user-selectable whole-app themes (dark, light, soft pink/green/brown) with persistence + OS default; reusable UI primitives; every screen and the nav restyled; dead scaffold CSS removed |
| 12 | `aws-deployment` | ⏸️ Deferred (no budget) | Dockerfile, S3 + CloudFront for frontend, RDS PostgreSQL, ECS Fargate, GitHub Actions CI/CD |

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
                 │   ├─ 09 (PDF import)
                 │   └─ 10 (inline deck/course creation)
                 └─ 08 (spaced repetition) ← uses SM-2 fields from 04

11 (frontend facelift) ← restyles the whole frontend; runs after the screens exist
12 (AWS deploy)        ← deferred (no budget); after everything else
```

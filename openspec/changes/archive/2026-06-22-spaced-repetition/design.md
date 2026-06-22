## Context

Change 05's study loop is entirely client-side (shuffle, reveal, round-robin) and the backend
only records a session summary (`study_sessions`). The `Card` entity holds content only; per-user
study state was deliberately deferred to this change so that PUBLIC decks studied by many users
can schedule independently per user.

Decisions taken with the user before this proposal:
- Grading UX is **Again / Hard / Good / Easy** (Anki-style), mapped onto SM-2 internally.
- Spaced repetition **extends the existing study mode** (a due-based review mode), rather than a
  separate flow.

## Goals / Non-Goals

**Goals:**
- Per-user SM-2 state with lazy creation, so a shared card schedules independently per user.
- Grade a card → next due date; review what is due, plus new cards.
- A small progress summary, and a due-review mode in the study UI.

**Non-Goals:**
- Daily new-card / review caps (Anki-style limits) — can come later.
- A cross-deck "study everything due" queue — the due queue is per deck; progress gives global
  counts only.
- Sub-day scheduling (learning steps in minutes). Scheduling is day-grained.
- Changing the existing session-summary recording or the `Card` entity.

## Decisions

### 1. `UserCardState` table (one row per user+card, lazy)
```
user_card_state
  id               BIGINT identity PK
  user_id          BIGINT NOT NULL  -> users(id)  ON DELETE CASCADE
  card_id          BIGINT NOT NULL  -> cards(id)  ON DELETE CASCADE
  ease_factor      DOUBLE PRECISION NOT NULL DEFAULT 2.5
  interval_days    INTEGER      NOT NULL DEFAULT 0
  repetitions      INTEGER      NOT NULL DEFAULT 0
  due_at           TIMESTAMPTZ  NOT NULL
  last_reviewed_at TIMESTAMPTZ  NOT NULL
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
  updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
  UNIQUE (user_id, card_id)
  INDEX (user_id, due_at)
```
Migration `V7__user_card_state.sql`; the entity mirrors it (ddl-auto=validate). A missing row
means "new"; the row is created on first review. *Why a row per user:* the same shared card must
schedule independently per user, so state cannot live on `Card`.

### 2. SM-2 algorithm and the 4-button mapping
Grades map to an SM-2 quality `q`: **Again → 1, Hard → 3, Good → 4, Easy → 5** (`q < 3` is a lapse).

- Ease update (all grades): `EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))`, floored at **1.3**.
- Lapse (`Again`): `repetitions = 0`, `interval = 1`.
- Pass (`q >= 3`): `repetitions += 1`; `interval = 1` if reps becomes 1, `6` if reps becomes 2,
  else `round(previousInterval * EF')`.
- `dueAt = now + interval days`; `lastReviewedAt = now`. Defaults for a new card: `EF 2.5`,
  `repetitions 0`, `interval 0`.

Implemented as a pure `Sm2Scheduler` (input: current state + grade + now → new state) so it is
unit-testable without the DB. *Alternative considered:* classic 0–5 input — rejected per the UX
decision; the mapping keeps the algorithm faithful while the UI stays simple.

### 3. Review endpoint nested under the card path
`POST /api/courses/{courseId}/decks/{deckId}/cards/{cardId}/review` with body `{ "grade": "GOOD" }`.
Access is resolved like study/recording: load the deck, `courseService.getReadable(courseId, ...)`
(404 if not readable), confirm the card is in the deck. The service upserts state via
`findByUserIdAndCardId` (creating it new if absent) and returns the new interval + due date.
Reviewable on any **readable** deck (including PUBLIC), matching study access.

### 4. Due queue = overdue + new
`GET /api/courses/{courseId}/decks/{deckId}/due` returns, for the current user, the deck's cards
that are new (no state row) or whose `due_at <= now`. Implemented with a repository query joining
`cards` to the user's `user_card_state` (left join: state NULL → new; or `due_at <= now`). New
cards sort first, then by due date. Returns plain card content (the same `CardResponse` shape) so
the client can run the existing flip UI.

### 5. Progress summary from `user_card_state`
`GET /api/study/progress` returns `{ dueNow, inReview, reviewedToday }` for the user, computed by
count queries over their state rows (`due_at <= now`; total rows; `last_reviewed_at >= startOfToday`).
"Today" uses a UTC day boundary, consistent with the month-to-date quota logic from change 06.

### 6. Grade as an enum; validation
`Grade` enum `{ AGAIN, HARD, GOOD, EASY }`; an unknown/missing grade is a 400 (bean validation /
enum binding). The review request carries only the grade — the card and deck come from the path.

### 7. Frontend extends the study screen
The study screen gains a "Review due" entry that calls the due-queue API, runs the existing
flip-card loop, and replaces the correct/missed self-grade with four buttons (Again/Hard/Good/Easy);
each selection POSTs a review before advancing. A separate progress view shows the three counts.
`Again` cards may also be re-queued within the session client-side, independent of their (next-day)
due date.

## Risks / Trade-offs

- **Time-zone "today" boundary** → use a single UTC instant for `due_at` and a UTC start-of-day for
  "reviewed today", matching change 06; documented as day-grained.
- **Large decks** → the due queue could be large; cap/paginate it (reuse `PagedResponse` or a sane
  limit) so a single review session is bounded.
- **Ease-factor drift** → floor at 1.3 (SM-2 standard) prevents runaway short intervals.
- **Migration must match the entity** (ddl-auto=validate) → write `V7` and the entity together and
  run the suite on H2 + verify on PostgreSQL.
- **Interaction with session summaries** → SR review is per-card and independent; the existing
  `study_sessions` summary recording is untouched and optional for SR mode.

## Migration Plan

- Additive: `V7__user_card_state.sql` creates one new table; no backfill (rows appear lazily on
  first review). No change to existing tables.
- Rollback: drop `user_card_state` (no other table depends on it).

## Open Questions

- Daily new-card and review limits — deferred (Non-Goal for now).
- Whether to expose a cross-deck "all due" study queue in addition to per-deck — deferred; progress
  counts cover the global view for now.

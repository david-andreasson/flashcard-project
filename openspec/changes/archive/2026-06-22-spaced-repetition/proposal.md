## Why

Change 05 gave a self-graded study loop but no memory: every session reshuffles all cards and
nothing is scheduled. This change adds per-user spaced repetition (SM-2), so each user reviews a
card on a schedule driven by their own recall — studying what is due rather than everything.

## What Changes

- Add a per-user `UserCardState` table (ease factor, interval, repetition count, next-due date),
  created lazily on a user's first review of a card. One row per (user, card), so the same shared
  card schedules independently for each user.
- Add SM-2 scheduling: grading a card Again / Hard / Good / Easy updates its state and computes
  the next due date; the ease factor never drops below its floor.
- Add a review endpoint: `POST /courses/{courseId}/decks/{deckId}/cards/{cardId}/review` with a
  grade → applies SM-2, upserts the user's state, returns the new interval and next-due date.
- Add a due queue: `GET /courses/{courseId}/decks/{deckId}/due` returns the user's due and new
  (never-reviewed) cards for a readable deck.
- Add a progress summary: `GET /study/progress` with counts (due now, in review, reviewed today).
- Extend the study frontend with a due-review mode (grade Again/Hard/Good/Easy, persisted per
  card via the review API) and a progress dashboard.

## Capabilities

### New Capabilities
- `spaced-repetition`: per-user SM-2 review state, card grading/review, the due-card queue, and
  progress counts.

### Modified Capabilities
- `study-mode-frontend`: add a spaced-repetition review mode and a progress view.

## Impact

- Backend: new `UserCardState` entity + repository, an `Sm2Scheduler`, a review service + endpoint,
  a due-queue endpoint, and a progress endpoint; Flyway migration `V7__user_card_state.sql`.
- Database: new `user_card_state` table (FKs to `users` and `cards`, both `ON DELETE CASCADE`),
  unique `(user_id, card_id)`, index `(user_id, due_at)`.
- Frontend: a due-review mode in the study screen and a progress dashboard, via `apiFetch`.
- Unchanged: the `Card` entity (content only) and the existing study-session summary recording.

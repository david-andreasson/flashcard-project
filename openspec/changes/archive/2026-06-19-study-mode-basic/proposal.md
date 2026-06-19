## Why

Decks now hold real cards, but there is no way to actually *study* them — only to view and
edit content. This change adds the core study experience: a flip-card loop where you see a
card's front, recall the answer, reveal the back, and self-grade. It turns the app from a
content manager into something you can learn with. Spaced-repetition scheduling (which cards
are due, SM-2) is intentionally left for change 08; this is the basic, always-available
study mode.

## What Changes

- **Frontend study loop (client-side)**: a "Study" entry point on a deck fetches its cards
  (reusing the change-04 list API), shuffles them, and runs the flip-card loop in the browser:
  show front → reveal back → self-grade ✓/✗. Missed cards are re-queued (round-robin) until
  all are answered correctly, then an end-of-session summary is shown.
- **Lightweight session persistence**: a new `StudySession` entity records a completed study
  run (user, deck, total cards, correct count, finished time) so the user has a simple study
  history. This is the only server state this change adds.
- **Study session API**: `POST` to record a finished session and `GET` to list a user's
  recent sessions (their own only — ownership-scoped). No per-card or per-session card state.
- **Study history view**: a small frontend view listing recent study sessions and their
  scores.

## Capabilities

### New Capabilities

- `study-session`: The StudySession domain — recording a completed study run (deck, totals,
  timestamp), listing a user's own session history, and the ownership rules around it.
- `study-mode-frontend`: The React flip-card study loop (shuffle, reveal, self-grade,
  round-robin re-queue, summary) and the study-history view.

### Modified Capabilities

## Impact

- Adds Flyway migration `V5__study_sessions.sql` (study_sessions table, FK to users and decks).
- Reuses the change-04 card list API for the study queue; reuses the change-03 ownership
  pattern (you can study any deck you can read — your own or public).
- Reuses `PagedResponse<T>`, `AuthPrincipal`, and the `common` exception handler.
- Does NOT add SM-2, due dates, `UserCardState`, or any per-card scheduling — that is
  change 08. A study session here is a one-off self-graded pass, not a scheduled review.

## Context

Cards exist (change 04) and are readable under the ownership/visibility model (own or public).
This change adds a study experience on top, with no new scheduling logic. Two decisions were
settled in exploration: the study loop runs **client-side**, and a **lightweight StudySession**
is persisted for history. SM-2 / due-date scheduling and per-card state are explicitly change 08.

## Goals / Non-Goals

**Goals:**
- A usable flip-card study loop: front → reveal back → self-grade ✓/✗, round-robin re-queue of
  missed cards, end-of-session summary
- Persist a completed session (deck, totals, timestamp) so the user has a study history
- Study any deck the user can read (own or public); history is the user's own

**Non-Goals:**
- SM-2, ease factors, intervals, due dates — change 08
- `UserCardState` / per-card persistent progress — change 08
- Server-managed card-by-card sessions (the loop is client-side)
- Editing cards while studying (that is the change-04 editor)
- Leaderboards, streaks, cross-user stats

## Decisions

### D1 — Client-side study loop

**Chosen**: The browser fetches the deck's cards (change-04 list API), shuffles, and drives the
whole loop (front/back/grade/re-queue) locally. The server is only touched to record the result.
**Alternative**: Server creates a session and serves cards one at a time, tracking position.

Without SM-2 there is nothing the server needs to compute per card. A client loop is far
simpler, needs no per-session card state, and is fully responsive. The server stays a thin
recorder of outcomes.

### D2 — Round-robin with self-grading

**Chosen**: After revealing the back, the user marks ✓ (got it) or ✗ (missed). Missed cards go
to the back of the queue and reappear until answered correctly; the session ends when the queue
is empty. The summary reports total unique cards and how many were correct on first try.
**Alternative**: A single shuffled pass with no re-queue.

Re-queuing missed cards is a real, simple study technique and matches the roadmap's
"random/round-robin". It needs no persistence — it is queue manipulation in the client.

### D3 — Lightweight `StudySession` persistence

**Chosen**: `study_sessions(id, user_id, deck_id, total_cards, correct_count, finished_at)`.
The client `POST`s once when a session finishes; `GET` lists the user's recent sessions. No
per-card rows, no in-progress sessions.
**Alternative**: No persistence (fully ephemeral), or a richer per-card session log.

A session-level summary is enough for a basic history ("you studied Spanish, 15/18") and is
independent of SM-2, so it does not conflict with change 08. Per-card history belongs to the
SM-2 work, not here.

### D4 — Ownership: study what you can read; history is your own

**Chosen**: You may start a study session for any deck you can read (own or public) — the
client just needs the cards, which the change-04 API already gates. A `StudySession` is owned
by the user who created it; listing returns only the caller's sessions (`findByUserId`).
Recording a session requires the deck to be readable by the caller.
**Alternative**: Allow recording sessions for decks you cannot read (rejected — pointless and
leaks deck existence).

This reuses the established read-scoping (own-or-public) for the deck and owner-scoping for the
session records — no new authorization concept.

## Risks / Trade-offs

- [Client trust: a user could POST any total/correct numbers] → Acceptable for a personal
  study history (no leaderboard, no stakes). The server validates that `correct_count <=
  total_cards` and that the deck is readable; it does not re-derive the score. Noted as a known
  limitation; SM-2 (change 08) will track real per-card outcomes server-side.
- [Large decks shuffled client-side] → Fine; decks are small (tens of cards). Pagination on the
  card fetch still applies (fetch with a large page size or page through).
- [No resume of an interrupted session] → Out of scope; a closed tab simply means no recorded
  session. Acceptable for basic mode.

## Open Questions

- None blocking. Whether to show richer history stats (per-deck averages, charts) is a later
  enhancement; this change ships a simple recent-sessions list.

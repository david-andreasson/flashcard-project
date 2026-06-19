## 1. Database migration

- [x] 1.1 Create `V5__study_sessions.sql`: `study_sessions` table (id, user_id FK → users ON DELETE CASCADE, deck_id FK → decks ON DELETE CASCADE, total_cards INT, correct_count INT, finished_at TIMESTAMPTZ, created_at)
- [x] 1.2 Add index `study_sessions(user_id)` (history is listed per user, newest first)

## 2. Study session domain

- [x] 2.1 Create `StudySession` JPA entity (userId, deckId, totalCards, correctCount, finishedAt, createdAt) matching the migration so `ddl-auto=validate` passes
- [x] 2.2 Create `StudySessionRepository`: `findByUserId(pageable)` ordered by finishedAt desc
- [x] 2.3 Implement `StudySessionService`: record (verify the deck is readable via `CourseService`/`DeckService` resolution through the deck's course; validate `0 <= correctCount <= totalCards`), and list-mine

## 3. Study session endpoints

- [x] 3.1 Create `StudySessionController` (`/study-sessions`): `POST` record (body: deckId, totalCards, correctCount) → 201; `GET` paginated list of the caller's sessions (newest first)
- [x] 3.2 Add request DTO with Bean Validation (deckId required, totalCards >= 0, correctCount >= 0) plus the cross-field check (correctCount <= totalCards) in the service → 400; add a `StudySessionResponse` DTO
- [x] 3.3 Reuse `@AuthenticationPrincipal AuthPrincipal`, `PagedResponse`, and the `common` exception handler

## 4. Backend tests

- [x] 4.1 Record + list: record a session for an owned deck, then list and see it (newest first)
- [x] 4.2 Readability: recording for a PUBLIC deck the user does not own succeeds; for a PRIVATE deck they do not own → 404
- [x] 4.3 Privacy: a user's list never includes another user's sessions
- [x] 4.4 Validation: correctCount > totalCards → 400; negative values → 400

## 5. Frontend — study loop

- [x] 5.1 Add study-session API helpers in `src/lib` (record session, list my sessions)
- [x] 5.2 Create a `StudyPage` (route `/courses/:courseId/decks/:deckId/study`): fetch the deck's cards (reuse `listCards`), shuffle into a queue, empty-deck state
- [x] 5.3 Flip-card UI: show front, "Show answer" reveals back (+ notes); only front visible before reveal
- [x] 5.4 Self-grade ✓/✗: correct removes the card from the queue; missed re-queues it to the back (round-robin); track first-try correct count
- [x] 5.5 End-of-session summary (total unique cards, correct count) + `POST` to record the session; handle the record call failing gracefully
- [x] 5.6 Add a "Study" button on the deck cards view (`DeckCardsPage`) linking to the study route

## 6. Frontend — history

- [x] 6.1 Create a `StudyHistoryPage` (route `/study-history`) listing recent sessions (deck title + score + date), paginated; add a nav link in `RootLayout`
- [x] 6.2 Resolve deck titles for display (include enough in the session response, or look up) — keep it simple

## 7. Verification

- [x] 7.1 Run `./mvnw verify` — all backend tests pass (23/23 green)
- [x] 7.2 Backend booted against PostgreSQL: `V5` migration applied and `ddl-auto=validate` passed
- [x] 7.3 Recorded a study session for the seeded public deck ("Greetings & Basics", 10/7); it appears in the history list with its deck title — verified live via curl
- [x] 7.4 Recording for a PRIVATE deck not owned by the caller → 404; `correctCount > totalCards` → 400 — verified live via curl
- [x] 7.5 History list works (backend, live); front-only-before-reveal is the frontend `showBack` state (code + build verified); the new study route is guarded (redirects to login when unauthenticated); no console errors. (Authed study UI not drivable in the preview browser — same fetch-cookie limitation as prior changes.)

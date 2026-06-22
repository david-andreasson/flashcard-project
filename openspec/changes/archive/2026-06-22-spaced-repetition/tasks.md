## 1. Persistence: UserCardState

- [x] 1.1 Add Flyway `V7__user_card_state.sql`: table with `user_id`/`card_id` FKs (both `ON DELETE CASCADE`), `ease_factor`, `interval_days`, `repetitions`, `due_at`, `last_reviewed_at`, timestamps; `UNIQUE (user_id, card_id)`; index `(user_id, due_at)`.
- [x] 1.2 Add `UserCardState` entity mirroring the migration (Lombok, `@CreationTimestamp`/`@UpdateTimestamp`), and a constructor for a fresh row.
- [x] 1.3 Add `UserCardStateRepository`: `findByUserIdAndCardId`; the due-queue query (cards in a deck that are new or `due_at <= now` for the user); count queries for progress (due now, in review, reviewed since a timestamp).

## 2. SM-2 scheduler

- [x] 2.1 Add `Grade` enum `{ AGAIN, HARD, GOOD, EASY }` with its SM-2 quality mapping (1/3/4/5).
- [x] 2.2 Add a pure `Sm2Scheduler`: given current state (ease, interval, repetitions), a grade, and `now`, return the next state (ease floored at 1.3; lapse resets reps + interval=1; pass grows interval 1 → 6 → round(interval×ease)); compute `dueAt`.

## 3. Review a card

- [x] 3.1 DTOs: `ReviewRequest(@NotNull Grade grade)` and `ReviewResponse(intervalDays, dueAt, repetitions, easeFactor)`.
- [x] 3.2 `SpacedRepetitionService.review(...)`: resolve readable deck (`getReadable` → 404) and card-in-deck; upsert state via `findByUserIdAndCardId` (lazy create); apply `Sm2Scheduler`; save; return the new schedule.
- [x] 3.3 `SpacedRepetitionController`: `POST /courses/{courseId}/decks/{deckId}/cards/{cardId}/review`.

## 4. Due-card queue

- [x] 4.1 `SpacedRepetitionService.due(...)` returning a readable deck's due + new cards for the user (new first, then by due date), bounded in size.
- [x] 4.2 Endpoint `GET /courses/{courseId}/decks/{deckId}/due` returning the cards as `CardResponse` (reuses the existing card shape so the client runs the flip UI).

## 5. Progress summary

- [x] 5.1 DTO `ProgressResponse(dueNow, inReview, reviewedToday)`.
- [x] 5.2 `GET /study/progress`: counts from the user's `user_card_state` (`due_at <= now`; total; `last_reviewed_at >=` UTC start-of-today).

## 6. Frontend (extend study mode)

- [x] 6.1 API client: `getDueCards(courseId, deckId)`, `reviewCard(courseId, deckId, cardId, grade)`, `getProgress()`.
- [x] 6.2 Add a "Review due" mode to the study screen: load due cards, reuse the flip loop, replace correct/missed with Again/Hard/Good/Easy; POST a review per card before advancing; handle "nothing due".
- [x] 6.3 Progress dashboard view (due now / in review / reviewed today) + route and nav entry.

## 7. Tests

- [x] 7.1 `Sm2Scheduler` unit tests: a pass grows the interval (1 → 6 → ×ease); Again resets reps and shortens; ease never drops below 1.3; quality mapping.
- [x] 7.2 Review integration: first review lazily creates state (HTTP 200 + next due); unreadable deck → 404; missing/invalid grade → 400; two users review the same card → independent state.
- [x] 7.3 Due-queue integration: new cards and overdue cards are returned; future-due cards excluded; unreadable deck → 404.
- [x] 7.4 Progress integration: counts reflect only the caller's state (due now / in review / reviewed today).

## 8. Config, docs, verification

- [x] 8.1 Update `docs/ARCHITECTURE.md`: mark `UserCardState` as implemented in change 08 (add `repetitions`/`lastReviewedAt`), correcting the earlier "(change 04)" note; document the SM-2 mapping briefly.
- [x] 8.2 Mark change 08 done on `docs/ROADMAP.md`.
- [x] 8.3 Run `./mvnw test` and the frontend build; confirm all green (PostgreSQL migration validates against the entity).

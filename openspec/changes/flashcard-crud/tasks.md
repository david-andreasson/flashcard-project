## 1. Database migration

- [x] 1.1 Create `V4__cards.sql`: `cards` table (id, deck_id FK → decks ON DELETE CASCADE, front/back VARCHAR(2000) NOT NULL, notes VARCHAR(4000) NULL, created_at, updated_at). Used VARCHAR (not TEXT) so `ddl-auto=validate` matches cleanly.
- [x] 1.2 Add index `cards(deck_id)`

## 2. Card domain

- [x] 2.1 Create `Card` JPA entity (deck_id Long, front, back, notes nullable, timestamps) matching the migration so `ddl-auto=validate` passes
- [x] 2.2 Create `CardRepository`: `findByDeckId(pageable)`, `findByIdAndDeckId` (+ `existsByDeckIdAndFront` for seeding)
- [x] 2.3 Implement `CardService`: resolve the parent deck+course access first (read via course readable, write via course owned-or-ADMIN), then act on the card; not-accessible → 404

## 3. Card endpoints

- [x] 3.1 Create `CardController` nested at `/courses/{courseId}/decks/{deckId}/cards`: POST create, GET paginated list (ordered by createdAt ASC), GET /{cardId}, PUT /{cardId}, DELETE /{cardId}
- [x] 3.2 Add request DTOs (create/update: front + back required, notes optional, length caps) with Bean Validation, and a `CardResponse` DTO
- [x] 3.3 Reuse `@AuthenticationPrincipal AuthPrincipal`, `PagedResponse`, and the `common` exception handler (404/403)

## 4. Content seeding

- [x] 4.1 Create `resources/seed/public-content.json`: a few public courses (by title) → decks → cards (~8-10 cards each for 3 courses)
- [x] 4.2 Added a `ContentSeeder` that reads the JSON and, for the admin-owned public courses, inserts any missing decks and cards (idempotent, keyed by course title → deck title → card front)
- [x] 4.3 Keep the "skip until admin exists" behavior; `@Order(1)` PublicCourseSeeder then `@Order(2)` ContentSeeder

## 5. Backend tests

- [x] 5.1 Card CRUD under an owned course: create/list succeed
- [x] 5.2 Visibility: anyone reads cards of a deck in a PUBLIC course; non-owner gets 404 on a PRIVATE course's cards
- [x] 5.3 Write protection: non-owner create → 404; ADMIN may modify any card
- [x] 5.4 Validation: blank front → 400
- [x] 5.5 Cascade: deleting a deck makes its cards inaccessible (404); physical FK cascade verified live on PostgreSQL
- [x] 5.6 Pagination: list returns `PagedResponse` ordered by createdAt ASC
- [x] 5.7 Seeder: seeded public courses contain their decks + cards (10 cards); no duplicates on a second run

## 6. Frontend

- [x] 6.1 Add typed card API helpers in `src/lib/courses.ts`: list/create/update/delete cards under a deck
- [x] 6.2 Create a deck cards view (route `/courses/:courseId/decks/:deckId`) listing cards (front/back/notes) with empty state
- [x] 6.3 Owner-only card editor: add-card form + per-card inline edit + delete (confirm on delete); hide controls for non-owned courses
- [x] 6.4 Link decks in `CourseDetailPage` to their cards view; wire the new route under `ProtectedRoute`
- [x] 6.5 Determine ownership in the UI by comparing the course's `ownerId` to `useAuth().user.id`

## 7. Verification

- [x] 7.1 Run `./mvnw verify` — all backend tests pass (19/19 green)
- [x] 7.2 Backend booted against PostgreSQL: `V4` migration applied and `ddl-auto=validate` passed; content seeding created 3 decks + 28 cards
- [x] 7.3 Seeded public course "Spanish Vocabulary" → "Greetings & Basics" shows 10 cards, readable by a fresh non-owner — verified live via curl
- [x] 7.4 As a normal user: added a deck and cards, edited (200) and deleted (204) a card, blank front → 400 — verified live via curl
- [x] 7.5 Non-owner read-only is the frontend `isOwner` check (code + build verified); the new deck-cards route is guarded (verified: redirects to login when unauthenticated); no console errors

<!-- Verification note: as in changes 02-03, the preview/automation browser does not persist
cookies set via fetch responses, so the authenticated cards UI could not be driven end-to-end
in that browser. All card behavior (CRUD, visibility 404s, validation, admin override, ordering,
seeding) was verified live against PostgreSQL via curl; the frontend builds clean and the new
route is guarded. A real user browser stores the cookies. -->


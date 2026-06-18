## 1. Database migration

- [x] 1.1 Create `V3__courses_and_decks.sql`: `courses` table (id, owner_id FK → users, title, visibility VARCHAR with CHECK in ('PUBLIC','PRIVATE') default 'PRIVATE', created_at, updated_at)
- [x] 1.2 In the same migration, `decks` table (id, course_id FK → courses ON DELETE CASCADE, title, created_at, updated_at)
- [x] 1.3 Add indexes: `courses(owner_id)`, `courses(visibility)`, `decks(course_id)`

## 2. Course domain

- [x] 2.1 Create `Visibility` enum (PUBLIC, PRIVATE)
- [x] 2.2 Create `Course` JPA entity (owner_id Long, title, visibility, timestamps) matching the migration so `ddl-auto=validate` passes
- [x] 2.3 Create `CourseRepository` with ownership-scoped queries: `findByIdAndOwnerId`, `findByOwnerId(pageable)`, `findByVisibility(pageable)`, and a read query `findReadable(id, ownerId)` (owner OR public)
- [x] 2.4 Implement `CourseService`: create (owner = caller, default PRIVATE), readOne (owned-or-public else 404), list-mine, list-public, update/delete (owner-or-ADMIN else 404), guard PUBLIC visibility changes to ADMIN only

## 3. Deck domain

- [x] 3.1 Create `Deck` JPA entity (course_id Long, title, timestamps)
- [x] 3.2 Create `DeckRepository`: `findByCourseId(pageable)`, `findByIdAndCourseId`
- [x] 3.3 Implement `DeckService`: operations resolve the parent course's access first (read via course readable, write via course owned-or-ADMIN), then act on the deck; not-accessible → 404

## 4. Pagination

- [x] 4.1 Create a reusable `PagedResponse<T>` record (content, page, size, totalElements, totalPages) with a `from(Page<T>)` factory
- [x] 4.2 Add a small helper / `Pageable` resolver that caps page size at a configured maximum (e.g. 100) with a default of 20

## 5. Course endpoints

- [x] 5.1 Create `CourseController` (`/courses`): `POST` create, `GET /{id}` read, `GET ?scope=mine|public` paginated list, `PUT /{id}` update, `DELETE /{id}`
- [x] 5.2 Add request DTOs (create/update) with Bean Validation (title not blank, length cap) and a `CourseResponse` DTO (no entity leakage)
- [x] 5.3 Read the current user via `@AuthenticationPrincipal AuthPrincipal`; map service not-found/forbidden to 404/403 via a `CommonExceptionHandler`

## 6. Deck endpoints

- [x] 6.1 Create `DeckController` nested at `/courses/{courseId}/decks`: `POST` create, `GET` paginated list, `GET /{deckId}` read, `PUT /{deckId}` update, `DELETE /{deckId}`
- [x] 6.2 Add deck request DTOs + `DeckResponse`; validate title

## 7. Public course seeding

- [x] 7.1 Implement an idempotent seeder (`ApplicationRunner`) that, when an admin user exists, creates the official PUBLIC courses owned by the admin if missing (keyed by title)
- [x] 7.2 Make the seed list configurable or constant; decks seeded empty for now (cards come in change 04)

## 8. Backend tests

- [x] 8.1 Ownership tests: owner reads/updates/deletes own course; non-owner gets 404 on private course read/update/delete
- [x] 8.2 Visibility tests: any user reads a PUBLIC course; non-admin cannot set PUBLIC (403); admin can modify any course
- [x] 8.3 Cascade test: deleting a course deletes its decks
- [x] 8.4 Deck nesting tests: create/list/read/update/delete under owned vs non-owned course (404 path)
- [x] 8.5 Pagination test: list returns a `PagedResponse`; page size cap enforced
- [x] 8.6 Seeding test: public courses exist after seeding; no duplicates on a second run

## 9. Frontend

- [x] 9.1 Add typed API helpers for courses/decks in `src/lib` (list mine/public, CRUD), reusing `apiFetch`
- [x] 9.2 Create a `CoursesPage` (browse): my courses + public courses, paginated, each linking to detail; empty state + "new course"
- [x] 9.3 Create a `CourseDetailPage`: course info + paginated decks; owner-only edit/delete/add-deck controls
- [x] 9.4 Create course create/edit forms and deck create/edit forms (owner only), with confirm-on-delete
- [x] 9.5 Wire routes under `ProtectedRoute`: `/courses`, `/courses/:id`; add nav link in `RootLayout`
- [x] 9.6 Show management controls only when the current user owns the course (compare `ownerId` to `useAuth().user.id`)

## 10. Verification

- [x] 10.1 Run `./mvnw verify` — all backend tests pass (13/13 green)
- [x] 10.2 Start backend against PostgreSQL; `V3` migration applied and `ddl-auto=validate` passed
- [x] 10.3 Create a course + deck, list, edit; public seeded courses (10) visible read-only — verified live via curl. (UI cookie round-trip not exercisable in the preview browser — see note.)
- [x] 10.4 Admin edits a public course (200); a second user cannot read the first user's private course (404) — verified live via curl
- [x] 10.5 Pagination works (size=3 → total=10, pages=4) verified live; management-controls-hidden is the frontend `isOwner` check (code + build verified); `/courses` route guarding confirmed in browser (redirects to login)

<!-- Verification note: as in change 02, the preview/automation browser does not persist cookies
set via fetch responses, so the authenticated courses UI could not be driven end-to-end in that
browser. All backend behavior (CRUD, visibility read/write rules, ownership 404s, admin override,
pagination, seeding) was verified live against PostgreSQL via curl; the frontend builds clean,
routes are guarded (verified: /courses → login when unauthenticated), and ownership UI logic is
covered by code review + the backend ownership tests. A real user browser stores the cookies. -->


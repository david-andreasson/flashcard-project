## Why

With authentication in place, users need somewhere to put study material. This change adds
the core content hierarchy — Courses and the Decks inside them — and establishes the
ownership and visibility model the whole app depends on: users own private courses, and a
set of official PUBLIC courses is readable by everyone. The ownership-scoped access pattern
introduced here is reused by every later data-owning feature (cards, AI usage, study state).

## What Changes

- New `Course` entity and `courses` table: `owner_id`, `title`, `visibility` (PUBLIC | PRIVATE),
  timestamps.
- New `Deck` entity and `decks` table: `course_id`, `title`, timestamps; a deck inherits its
  course's visibility. `ON DELETE CASCADE` from course to decks.
- Course/Deck CRUD endpoints under `/api/courses` with **nested deck routes**
  (`/api/courses/{courseId}/decks`).
- **Ownership-scoped access**: READ a course/deck if `owner_id = me` OR `visibility = PUBLIC`;
  WRITE (create/update/delete) only if `owner_id = me` (ADMIN may write any). Not-accessible →
  HTTP 404 (does not reveal existence).
- **Pagination** on all list endpoints via a reusable `PagedResponse<T>` envelope.
- Seed ~10 official PUBLIC courses owned by the configured admin (content/cards arrive in
  change 04; decks may be seeded empty).
- Frontend: a course browser (my courses + public courses), a course detail view listing its
  decks, and create/edit/delete for owned courses and decks.

## Capabilities

### New Capabilities

- `course-management`: The Course domain — CRUD, the PUBLIC/PRIVATE visibility model, the
  ownership read/write rules, admin-owned public course seeding, and paginated listing.
- `deck-management`: The Deck domain — CRUD nested under a course, visibility inherited from
  the course, cascade delete with the course, and paginated listing.
- `course-deck-frontend`: React views — browse my + public courses, course detail with its
  decks, and owner-only create/edit/delete forms.

### Modified Capabilities

## Impact

- Adds Flyway migration `V3__courses_and_decks.sql` (courses, decks, indexes, FK cascade).
- Establishes the ownership-scoped repository pattern (`findByIdAndOwnerId`,
  read-or-public queries) reused by changes 04+.
- Reads the current user via `@AuthenticationPrincipal AuthPrincipal` (from change 02).
- Introduces `PagedResponse<T>` used by every later list endpoint.
- Frontend gains its first authenticated data views behind `ProtectedRoute`.
- Does NOT add Cards or study state — those are change 04 (`UserCardState` holds per-user
  SM-2 progress, since public decks are studied in place by many users).

## Context

Change 02 established auth: every request carries an `AuthPrincipal` (id, email, role, plan)
via the access-token cookie. This change adds the first user-owned data. Two product
decisions from exploration shape it:

1. **Courses can be PUBLIC or PRIVATE.** ~10 official PUBLIC courses (admin-owned) are
   readable by everyone; users also create PRIVATE courses. This splits the ownership rule:
   reads include public content, writes stay owner-only.
2. **Public courses are studied in place** (not forked). This is why per-user study progress
   (SM-2) will live in a separate `UserCardState` table in change 04 — but that is out of
   scope here; this change only builds Courses and Decks.

The ownership-scoped access pattern defined here is reused by every later data-owning feature.

## Goals / Non-Goals

**Goals:**
- Course + Deck CRUD with a clean, reusable ownership-enforcement pattern
- PUBLIC/PRIVATE visibility; public content readable by all, writable only by owner/admin
- Pagination on all list endpoints via a reusable envelope
- Seed ~10 admin-owned PUBLIC courses
- Frontend browse/detail/edit views behind `ProtectedRoute`

**Non-Goals:**
- Cards / flashcard content (change 04)
- Per-user study state / `UserCardState` / SM-2 (change 04 + 08)
- Forking/cloning a public course into a private copy (possible future feature)
- Sharing a private course with specific other users (only PUBLIC/PRIVATE for now)
- Moving a deck between courses (decks are created under one course)

## Decisions

### D1 — Ownership-scoped repository queries (read = owned-or-public, write = owned)

**Chosen**: Bake ownership into every query rather than loading then checking.
- Read one: `findByIdAndOwnerIdOrVisibility(id, me, PUBLIC)` → empty means 404
- List mine: `findByOwnerId(me, pageable)`; list public: `findByVisibility(PUBLIC, pageable)`
- Write: load via owner-scoped query `findByIdAndOwnerId(id, me)` → empty means 404

**Alternative**: `@PreAuthorize` SpEL, or service-layer load-then-compare.

Scoping the query makes it structurally impossible to read or mutate another user's data,
with no annotation to forget and no separate check to skip. ADMIN is allowed to write any
course (checked explicitly in the service when `principal.role() == ADMIN`).

### D2 — Visibility as an enum on Course; Deck inherits

**Chosen**: `visibility: PUBLIC | PRIVATE` column on `courses`. A deck's visibility is its
course's — no `visibility` column on `decks`.
**Alternative**: visibility on both; or a separate sharing table.

Decks always belong to exactly one course, so course visibility fully determines deck
visibility. Duplicating it invites inconsistency. Deck access is checked by resolving the
parent course's access first.

### D3 — Not-accessible returns 404, not 403

**Chosen**: A resource the user may not read (wrong owner, not public) returns 404.
**Alternative**: 403 Forbidden.

404 does not reveal whether an id exists, avoiding an enumeration oracle. It also falls out
naturally from D1 (the scoped query simply returns empty). 403 is reserved for CSRF/role
failures handled by Spring Security.

### D4 — Nested routes for decks

**Chosen**: `/api/courses/{courseId}/decks` for listing/creating decks in a course; single
deck operations at `/api/courses/{courseId}/decks/{deckId}`.
**Alternative**: flat `/api/decks/{id}` with `course_id` in the body, denormalizing
`owner_id` onto decks.

Nested routes mirror the hierarchy and how users think ("decks in this course"), keep the
data normalized (no `owner_id` on decks), and make access checks read naturally (resolve
course access, then the deck under it).

### D5 — `ON DELETE CASCADE` from course to decks

**Chosen**: Database-level cascade on the `decks.course_id` foreign key.
**Alternative**: JPA cascade / manual deletion in the service.

A DB cascade is atomic and can't be bypassed by a code path that forgets to clean up — the
same approach used for `refresh_tokens` in change 02. Later, `decks → cards` cascades the
same way.

### D6 — Reusable `PagedResponse<T>` envelope

**Chosen**: A small DTO `{ content, page, size, totalElements, totalPages }` returned by all
list endpoints; query params `?page=0&size=20` (sane default + max size cap).
**Alternative**: Return Spring Data's `Page<T>` directly.

Spring's `PageImpl` JSON shape is explicitly unstable across versions and logs a warning when
serialized. A project-owned envelope is stable and consistent for the frontend, and is reused
by every later list endpoint.

### D7 — Public courses seeded as admin-owned

**Chosen**: Seed the official PUBLIC courses owned by the configured `app.admin-email` user.
Seeding runs idempotently on startup (only inserts if missing), keyed by a stable title or
slug, so it survives restarts and is environment-portable.
**Alternative**: A Flyway data migration with hardcoded rows.

Tying seeds to the admin account keeps a real owner for writes/edits and avoids a null owner.
A startup seeder (vs. a data migration) lets the seed depend on the admin user existing and
stays easy to adjust. Actual card content arrives in change 04; decks may be seeded empty now.

## Risks / Trade-offs

- [ADMIN write-any bypasses owner scoping] → Explicit `role == ADMIN` branch in the service,
  covered by a test; keeps the common path strictly owner-scoped.
- [Seeding depends on the admin user existing] → Seeder is a no-op until an admin has
  registered; it runs idempotently on each startup and fills in when the admin appears.
- [Nested routes get verbose for deep single-item ops] → Acceptable; the hierarchy is only
  two levels (course → deck).
- [`PagedResponse` duplicates some Spring functionality] → Small, intentional, and stable;
  worth it for a consistent API contract.

## Open Questions

- None blocking. Whether public seed courses ship with starter decks now or stay empty until
  change 04 is a content decision, not an architectural one — default to empty decks.

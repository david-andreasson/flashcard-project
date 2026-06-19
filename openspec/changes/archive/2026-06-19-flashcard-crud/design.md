## Context

Change 03 built Courses and Decks with an ownership/visibility model (read = owned-or-public,
write = owned, ADMIN overrides) and a reusable `PagedResponse<T>`. This change adds the leaf
of the hierarchy — Cards — and fills the public courses with sample content.

A key decision was settled in the change-03 exploration and refined here: because public
decks are studied by many users, **per-user study progress (SM-2) lives in a separate
`UserCardState` table, not on the card**. This change builds only the shared `Card` content;
`UserCardState` is deferred to change 08, where SM-2 first writes to it (nothing reads or
writes per-user study state before then, so creating the table now would leave it unused).

## Goals / Non-Goals

**Goals:**
- `Card` entity + nested CRUD, reusing the change-03 ownership pattern and pagination
- Plain-text front / back / optional notes
- Owner-only card editor in the frontend; deck view lists cards
- Public courses seeded with real sample decks + cards (JSON-driven, idempotent)

**Non-Goals:**
- `UserCardState`, SM-2, due dates, "new vs reviewed" logic — change 08
- Study session / queue / flip UI — change 05
- Rich text / markdown / images in cards — plain text only for now
- Manual card ordering / drag-to-reorder — cards are ordered by `createdAt`
- AI card generation — change 07

## Decisions

### D1 — Card holds shared content only (front/back/notes, plain text)

**Chosen**: `cards(id, deck_id, front, back, notes nullable, created_at, updated_at)`, all text
plain. No study/scheduling fields on the card.
**Alternative**: put SM-2 fields on the card (rejected in change 03), or support markdown now.

Keeping the card to pure content is what makes shared public decks possible — every user
studies the same card row. Plain text avoids markdown rendering and XSS handling; it can be
upgraded later without a data change. `notes` is an optional hint/explanation field.

### D2 — Nested card routes, ownership inherited from the course

**Chosen**: `/api/courses/{courseId}/decks/{deckId}/cards`. Access resolves the course first
(read = owned-or-public, write = owned/ADMIN), then the deck within the course, then the card
within the deck. Not-accessible → 404.
**Alternative**: flat `/api/cards/{id}`.

This mirrors the change-03 nested deck routes and the hierarchy, keeps the ownership check a
straight extension of the existing `CourseService`/`DeckService` resolution, and needs no
`owner_id` denormalization on cards.

### D3 — Cards ordered by `createdAt`

**Chosen**: List cards ordered by `createdAt` (then `id`). No `position` column.
**Alternative**: explicit `position` with reordering.

Study mode (change 05) presents cards randomly or round-robin, so authored order is not
load-bearing yet. A `position` column + reorder UI is real complexity for no current benefit;
it can be added later without breaking existing data.

### D4 — `UserCardState` deferred to change 08

**Chosen**: This change does not create `UserCardState`. A card with no per-user state is
simply "new"; that table and the lazy-creation-on-first-review behavior land in change 08
with SM-2.
**Alternative**: create the table now (as the roadmap originally implied).

Nothing writes or reads per-user study state until SM-2 (change 08) — basic study (change 05)
iterates cards without persistent per-card state. Building the table now would leave it unused
for several changes. Deferring keeps change 04 cohesive (content only). The data-model
decision (progress is per-user, off the card) is unchanged — only the build timing moves.

### D5 — JSON-driven, idempotent content seeder

**Chosen**: A seed resource `resources/seed/public-content.json` describing public
courses → decks → cards. A startup seeder (extending the change-03 `PublicCourseSeeder`)
reads it and, for the admin-owned public courses, inserts any missing decks/cards. Idempotent,
keyed by stable titles.
**Alternative**: hardcode sample content in Java, or a Flyway data migration.

A JSON file keeps sample content out of code and easy to edit, and a startup seeder (vs. a
data migration) can depend on the admin user existing and stays adjustable. Not every public
course needs content — a few well-populated ones are enough to demonstrate the app.

### D6 — `ON DELETE CASCADE` deck → cards

**Chosen**: DB-level cascade on `cards.deck_id` (matching `decks.course_id` in change 03).
Deleting a deck removes its cards; deleting a course removes decks then cards.
**Alternative**: application-level deletion.

Consistent with the existing cascade chain, atomic, and impossible to bypass.

## Risks / Trade-offs

- [Seeder inserts duplicate cards if not keyed carefully] → Idempotency keyed by
  (course title → deck title → card front); only insert when the deck/card is missing. Covered
  by a "no duplicates on restart" test.
- [Plain text only may feel limited] → Acceptable for now; upgrading to markdown later is a
  rendering change, not a schema change.
- [Seeder couples to course titles from change 03] → The seed file references public courses by
  title; if a title changes, the seed simply no-ops for the missing one. Acceptable.

## Open Questions

- How many public courses get sample content and how many cards each — a content choice, not
  architectural. Default: 3–4 courses, one deck of ~10 cards each. Easy to expand later by
  editing the JSON.

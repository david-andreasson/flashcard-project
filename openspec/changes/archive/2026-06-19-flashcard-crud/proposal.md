## Why

Courses and decks exist but contain nothing — the 10 public courses are empty shells and a
user's decks are just titles. This change adds the actual study content: flashcards. After
this, decks have real cards you can create, edit, and view, and the official public courses
ship with sample content so the app is demonstrable end-to-end.

## What Changes

- New `Card` entity and `cards` table: `deck_id`, `front`, `back`, optional `notes`,
  timestamps. Card holds **shared content only** (plain text) — no per-user study state.
- Nested card CRUD under decks: `/api/courses/{courseId}/decks/{deckId}/cards`
  (list/create/read/update/delete), reusing the change-03 ownership pattern (read =
  owned-or-public, write = owned, ADMIN overrides) and `PagedResponse<T>`.
- Cards are ordered by `createdAt` (no manual ordering yet).
- Frontend: a deck view listing its cards and an owner-only card editor
  (front / back / notes) with create/edit/delete.
- A JSON-driven content seeder that fills a few of the public courses with sample decks and
  cards, run idempotently on startup. The change-03 course seeder is extended/replaced so
  public courses get real, visible content.

## Capabilities

### New Capabilities

- `flashcard-management`: The Card domain — nested CRUD within a deck, plain-text
  front/back/notes content, ordering, and the read/write ownership rules inherited from the
  course.
- `content-seeding`: A JSON-driven, idempotent seeder that populates public courses with
  sample decks and cards owned by the admin.
- `flashcard-frontend`: React deck-cards view and an owner-only card editor.

### Modified Capabilities

<!-- None. The "deleting a deck removes its cards" cascade is expressed from the card side in
     flashcard-management, so deck-management's spec is unchanged. -->

## Impact

- Adds Flyway migration `V4__cards.sql` (cards table, FK to decks `ON DELETE CASCADE`, index
  on `deck_id`).
- Extends/replaces `PublicCourseSeeder` with a JSON-resource-driven seeder
  (`resources/seed/public-content.json`).
- Reuses the ownership-scoped pattern and `PagedResponse<T>` from change 03; no new
  cross-cutting infrastructure.
- Does NOT add `UserCardState` or any SM-2 / study logic — per-user study state is created
  in change 08 (where SM-2 first writes to it); the study queue/flip UI is change 05.
- A card with no study state is simply "new"; that concept is realized in change 08.

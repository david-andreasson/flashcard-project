## Why

On the AI generate screen the user can only save drafts into a course and deck that already
exist. A user whose account has no suitable deck is stuck — they must leave the screen, create a
course and deck elsewhere, then come back. For AI-generated material (often a brand-new topic) the
natural moment to create the deck is right there at save time.

## What Changes

- The save step on the AI generate screen lets the user either pick an existing course/deck (as
  today) **or** create a new course and/or a new deck inline and save the selected drafts into it.
- Because a brand-new course has no decks, choosing "new course" also requires a new deck; "new
  deck under an existing course" is supported too. ("New course + existing deck" is impossible and
  the UI prevents it.)
- New courses are created PRIVATE (the existing backend default); new course/deck titles follow the
  existing validation (non-blank, ≤ 200 characters).
- Implemented entirely in the frontend save section by orchestrating endpoints that already exist
  (create course → create deck → bulk-save cards). A freshly created course/deck is immediately
  treated as the selected existing one, so retrying after a partial failure does not create
  duplicates.
- No backend, API-client, or database changes.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `ai-card-generation-frontend`: the "Save selected drafts to a deck" requirement expands so the
  user may create a new course and/or a new deck inline at save time and save into it.

## Impact

- Affected code: `frontend/src/components/AiGeneratePage.tsx` (save section only). Reuses
  `createCourse` / `createDeck` (in `frontend/src/lib/courses.ts`) and the existing bulk-save
  helper.
- No backend changes: `POST /courses`, `POST /courses/{courseId}/decks`, and the bulk-card endpoint
  are reused unchanged. No new dependencies, no database or migration changes.
- No automated frontend tests (the project has no frontend test runner); verified via the `tsc`
  build and manual end-to-end runs.

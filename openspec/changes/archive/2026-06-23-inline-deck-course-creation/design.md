## Context

The AI generate screen (`frontend/src/components/AiGeneratePage.tsx`) currently saves drafts only
into a course + deck the user already owns: two `<select>` dropdowns (course, then deck) and a Save
button that calls the bulk-card endpoint. The backend already exposes `POST /courses` (creates a
PRIVATE course — `CourseService.create`) and `POST /courses/{courseId}/decks`, both returning the
created entity with its `id` and both title-validated (`@NotBlank @Size(max = 200)`). The API client
(`frontend/src/lib/courses.ts`) already wraps them as `createCourse(title)` and
`createDeck(courseId, title)`. Inline creation therefore needs **no backend or API-client change** —
only the screen's save section.

## Goals / Non-Goals

**Goals:**
- Let the user create a new course and/or deck inline at save time and save the drafts into it,
  without leaving the screen.
- Keep the existing "existing course + existing deck" path working exactly as today.
- Avoid duplicate course/deck creation when a save is retried after a partial failure.

**Non-Goals:**
- No backend, API-client, or database changes.
- No new styling system — the page keeps its current inline-style approach; visual design belongs
  to the upcoming `frontend-facelift` change.
- No changes to course/deck management screens elsewhere.
- No automated frontend tests (the project has no frontend test runner).

## Decisions

### Decision 1: UX pattern — a "New…" sentinel option in each dropdown
Each of the course and deck `<select>`s gains a trailing `➕ New course…` / `➕ New deck…` option;
selecting it reveals a text input for the title.
- Alternatives: (a) a `+` button that swaps the select for an input; (b) an "Existing | New" radio
  pair per level.
- Why: the sentinel adds the least chrome, fits the existing single save row, and maps directly to
  "existing or new". (a) and (b) add layout for no functional gain. Visual polish is deferred to the
  facelift, so the cheapest workable control wins now.

### Decision 2: Enforce the course→deck dependency in the UI
A brand-new course has no decks, so "new course + existing deck" is impossible. When the user picks
"new course", the deck control becomes a required new-deck input and the existing-deck dropdown is
not offered. The three valid combinations are: existing+existing, existing+new, new+new.

### Decision 3: Orchestrate existing endpoints in the frontend, with a creation cache for retries
Save resolves the target with up to three sequential calls: `createCourse` (if new) →
`createDeck` (if new) → bulk-save cards. Each created course/deck is appended to the local list and
cached in a ref; if a later step fails, retrying the save reuses the cached course/deck instead of
creating it again. A ref cache is used rather than flipping the dropdown to the new id mid-save,
because changing the selected course re-triggers the deck-loading effect and would clobber the
in-flight selection. The cache is cleared when the user changes the target or on a successful save.
- Alternative: a new atomic backend endpoint (e.g. `POST /ai/cards/save`) creating course + deck +
  cards in one transaction.
- Why frontend orchestration: keeps the change small and backend-free. The creation cache means a
  retry after a failed card-save reuses the already-created course/deck instead of duplicating it.
  The atomic endpoint is rejected for this scope — it duplicates ownership/creation logic and grows
  the change for a marginal guarantee.

### Decision 4: Client validation mirrors the backend
New titles are trimmed and required, max 200 characters, before any request is sent — matching
`@NotBlank @Size(max = 200)`. The server stays the source of truth; the client check just avoids an
obvious failed round-trip.

## Risks / Trade-offs

- **An empty course/deck is left behind if the user creates one but abandons before a successful
  card save** → Harmless and user-deletable; the creation cache prevents duplicates on retry.
  Accepted for this scope.
- **The three requests are not atomic** → Each step surfaces its own error and the user can retry
  without duplication (Decision 3). True atomicity is explicitly out of scope.
- **A sentinel option mixes an action into a value control** → A common, accepted pattern; can be
  revisited if the facelift reworks these controls.

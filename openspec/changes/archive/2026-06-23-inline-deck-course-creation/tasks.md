## 1. Save-target state & API wiring

- [x] 1.1 Import `createCourse` and `createDeck` from `lib/courses` into `AiGeneratePage`.
- [x] 1.2 Add state for the course target (mode: existing | new, plus a new-course title) and the deck target (mode: existing | new, plus a new-deck title); represent the "new" choice via a sentinel value on each `<select>`.

## 2. Course & deck selection UI

- [x] 2.1 Add a trailing `➕ New course…` option to the course dropdown; when chosen, reveal a new-course title input.
- [x] 2.2 Add a trailing `➕ New deck…` option to the deck dropdown; when chosen, reveal a new-deck title input.
- [x] 2.3 Enforce the course→deck dependency: when "new course" is selected, force the deck into new-deck mode and do not offer the existing-deck dropdown.
- [x] 2.4 Mirror backend validation (trim, non-blank, ≤ 200 chars) and disable Save until a valid target is specified (valid existing ids, or required new titles present).

## 3. Save orchestration (promote-on-create)

- [x] 3.1 In `onSave`, if the course is new, call `createCourse(title)`, append it to the local course list, and cache the created course for reuse.
- [x] 3.2 If the deck is new, call `createDeck(courseId, title)`, append it to the local deck list, and cache the created deck for reuse.
- [x] 3.3 Call the existing bulk-save with the resolved courseId/deckId; on success keep the current saved banner and "Go to deck" link using the new ids.
- [x] 3.4 On failure, surface the error and reuse the cached course/deck on retry so no duplicates are created.

## 4. Verification

- [x] 4.1 `npm run build` (strict `tsc`) is clean.
- [x] 4.2 Manual: existing course + existing deck still saves (unchanged path).
- [x] 4.3 Manual: existing course + new deck creates the deck and saves into it.
- [x] 4.4 Manual: new course + new deck creates a PRIVATE course + deck and saves; "Go to deck" opens the new deck.
- [x] 4.5 Manual: a FREE user still sees the AI-unavailable message (plan gate unchanged).

# spaced-repetition Specification

## Purpose
TBD - created by archiving change spaced-repetition. Update Purpose after archive.
## Requirements
### Requirement: Per-user card review state
The system SHALL maintain spaced-repetition state per (user, card): an ease factor, an interval in
days, a repetition count, and a next-due timestamp. A card with no state for a user is "new".
State SHALL be created lazily on the user's first review of a card, and a shared card SHALL
schedule independently for each user.

#### Scenario: First review creates state
- **WHEN** a user reviews a card they have never reviewed
- **THEN** a review-state row is created for that (user, card) with a computed next-due date

#### Scenario: State is independent per user
- **WHEN** two users review the same shared card
- **THEN** each has their own state and next-due date, unaffected by the other

### Requirement: SM-2 scheduling from a grade
On review the user grades recall as one of Again, Hard, Good, or Easy. The system SHALL update the
card's state using the SM-2 algorithm: a failing grade (Again) SHALL reset the card to be seen
again soon, successful grades SHALL lengthen the interval according to the ease factor, and the
ease factor SHALL never drop below its defined floor.

#### Scenario: A successful grade lengthens the interval
- **WHEN** a user grades a previously-scheduled card Good or Easy
- **THEN** its next interval is longer than before and its next-due date moves further out

#### Scenario: Again resets the schedule
- **WHEN** a user grades a card Again
- **THEN** its repetition count resets and it becomes due again soon

### Requirement: Review a card
An authenticated user SHALL be able to submit a review for a card in a deck they can read, via
`POST /api/courses/{courseId}/decks/{deckId}/cards/{cardId}/review` with a grade. The response
SHALL include the updated interval and next-due date.

#### Scenario: Review a readable card
- **WHEN** a user posts a valid grade for a card in a deck they own or a PUBLIC deck
- **THEN** the review is applied and HTTP 200 returns the updated interval and next-due date

#### Scenario: Review in an unreadable deck is rejected
- **WHEN** a user posts a review for a card in a PRIVATE deck they do not own
- **THEN** the response is HTTP 404 and no state is created

#### Scenario: Invalid grade is rejected
- **WHEN** a user posts a review with a missing or unknown grade
- **THEN** the response is HTTP 400 and no state is created

### Requirement: Due-card queue
The system SHALL return the cards a user should review now for a readable deck — those whose
next-due date has passed for that user, plus new (never-reviewed) cards — via
`GET /api/courses/{courseId}/decks/{deckId}/due`.

#### Scenario: Due and new cards are returned
- **WHEN** a user requests the due queue for a readable deck
- **THEN** the response contains that deck's new cards and any cards whose next-due date has passed
  for that user

#### Scenario: Not-yet-due cards are excluded
- **WHEN** a card's next-due date for the user is in the future
- **THEN** it is not included in the due queue

#### Scenario: Due queue for an unreadable deck is rejected
- **WHEN** a user requests the due queue for a PRIVATE deck they do not own
- **THEN** the response is HTTP 404

### Requirement: Study progress summary
The system SHALL provide the authenticated user a summary of their review progress, via
`GET /api/study/progress`, including how many cards are due now, how many are in review (have
state), and how many were reviewed today.

#### Scenario: Progress reflects the user's own state
- **WHEN** a user requests their progress
- **THEN** the response includes due-now, in-review, and reviewed-today counts computed only from
  that user's review state


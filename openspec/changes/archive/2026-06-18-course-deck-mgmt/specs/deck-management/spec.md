## ADDED Requirements

### Requirement: Create a deck in a course
An authenticated user SHALL be able to create a deck within a course they own, via the nested
route `/api/courses/{courseId}/decks`. The deck belongs to that course.

#### Scenario: Owner creates a deck
- **WHEN** the course owner posts a valid title to `/api/courses/{courseId}/decks`
- **THEN** a deck is created under that course and HTTP 201 is returned

#### Scenario: Cannot create a deck in a course you do not own
- **WHEN** a user posts a deck to a course they do not own
- **THEN** the response is HTTP 404 (the course is not writable by them)

#### Scenario: Missing title is rejected
- **WHEN** the owner posts a deck with a blank title
- **THEN** the response is HTTP 400 and no deck is created

### Requirement: Deck visibility follows its course
A deck SHALL be readable exactly when its parent course is readable by the requester: the
course owner, or anyone if the course is PUBLIC. Decks have no independent visibility.

#### Scenario: Public course decks are readable by anyone
- **WHEN** any authenticated user lists decks of a PUBLIC course
- **THEN** the response is HTTP 200 with the decks

#### Scenario: Private course decks hidden from non-owner
- **WHEN** a user lists decks of a PRIVATE course they do not own
- **THEN** the response is HTTP 404

### Requirement: List decks in a course with pagination
The system SHALL list the decks of a course the requester may read, paginated using the shared
`PagedResponse` envelope.

#### Scenario: List decks of an accessible course
- **WHEN** a user lists decks of a course they may read with `?page=0&size=20`
- **THEN** the response is a `PagedResponse` of that course's decks

### Requirement: Update and delete owned decks
An authenticated user SHALL be able to update or delete a deck within a course they own.
Operating on a deck in a course they do not own SHALL behave as if it does not exist. ADMIN
users MAY modify any deck.

#### Scenario: Owner updates a deck
- **WHEN** the course owner updates a deck's title
- **THEN** the change is saved and HTTP 200 is returned

#### Scenario: Non-owner deck update is rejected as not found
- **WHEN** a user updates a deck in a course they do not own
- **THEN** the response is HTTP 404 and nothing changes

#### Scenario: Deleting a deck does not affect the course
- **WHEN** the owner deletes a deck
- **THEN** only that deck is removed; the course and its other decks remain

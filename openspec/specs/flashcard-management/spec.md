# Spec: flashcard-management

## Purpose

Defines the Card domain: nested CRUD within a deck, plain-text front / back / optional-notes
content, createdAt ordering, and the read/write ownership rules inherited from the course.
## Requirements
### Requirement: Create a card in a deck
An authenticated user SHALL be able to create a card in a deck within a course they own, via
`/api/courses/{courseId}/decks/{deckId}/cards`. A card has a `front` and a `back` (required)
and optional `notes`, all plain text.

#### Scenario: Owner creates a card
- **WHEN** the course owner posts a valid front and back to a deck's cards endpoint
- **THEN** a card is created in that deck and HTTP 201 is returned

#### Scenario: Missing front or back is rejected
- **WHEN** the owner posts a card with a blank front or back
- **THEN** the response is HTTP 400 and no card is created

#### Scenario: Cannot add a card to a deck in a course you do not own
- **WHEN** a user posts a card to a deck whose course they do not own
- **THEN** the response is HTTP 404

### Requirement: Card visibility follows the course
A card SHALL be readable exactly when its deck's course is readable by the requester (the
course owner, or anyone if the course is PUBLIC). Cards have no independent visibility.

#### Scenario: Public course cards are readable by anyone
- **WHEN** any authenticated user lists the cards of a deck in a PUBLIC course
- **THEN** the response is HTTP 200 with the cards

#### Scenario: Private course cards hidden from non-owner
- **WHEN** a user lists cards of a deck in a PRIVATE course they do not own
- **THEN** the response is HTTP 404

### Requirement: List cards in a deck with pagination
The system SHALL list a deck's cards, ordered by creation time, paginated using the shared
`PagedResponse` envelope.

#### Scenario: List cards of an accessible deck
- **WHEN** a user lists cards of a deck they may read with `?page=0&size=20`
- **THEN** the response is a `PagedResponse` of that deck's cards ordered by `createdAt`

### Requirement: Update and delete owned cards
An authenticated user SHALL be able to update or delete a card in a course they own. Operating
on a card in a course they do not own SHALL behave as if it does not exist. ADMIN users MAY
modify any card.

#### Scenario: Owner updates a card
- **WHEN** the course owner updates a card's front, back, or notes
- **THEN** the change is saved and HTTP 200 is returned

#### Scenario: Non-owner card update is rejected as not found
- **WHEN** a user updates a card in a course they do not own
- **THEN** the response is HTTP 404 and nothing changes

### Requirement: Deleting a deck removes its cards
When a deck is deleted, all of its cards SHALL be removed. When a course is deleted, all of its
decks and their cards SHALL be removed.

#### Scenario: Deleting a deck deletes its cards
- **WHEN** the owner deletes a deck that contains cards
- **THEN** the deck and all its cards are removed

#### Scenario: Deleting a course deletes decks and cards
- **WHEN** the owner deletes a course containing decks and cards
- **THEN** the course, its decks, and all their cards are removed

### Requirement: Bulk-create cards in a deck
An authenticated user SHALL be able to create multiple cards in a deck within a course they own in
a single request, via `POST /api/courses/{courseId}/decks/{deckId}/cards/bulk`. Each item has a
required `front` and `back` and optional `notes`, all plain text. The same ownership rules as
single card creation SHALL apply, and ADMIN users MAY create cards in any course.

#### Scenario: Owner bulk-creates cards
- **WHEN** the course owner posts a list of valid cards to a deck's bulk endpoint
- **THEN** all cards are created in that deck and HTTP 201 is returned

#### Scenario: Bulk-create into a deck in a course you do not own
- **WHEN** a user posts bulk cards to a deck whose course they do not own
- **THEN** the response is HTTP 404 and no cards are created

#### Scenario: An invalid item rejects the whole batch
- **WHEN** the owner posts a batch in which any item has a blank front or back
- **THEN** the response is HTTP 400 and no cards from the batch are created


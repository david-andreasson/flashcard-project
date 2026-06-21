## ADDED Requirements

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

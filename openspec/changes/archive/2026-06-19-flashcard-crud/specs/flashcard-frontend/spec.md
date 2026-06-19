## ADDED Requirements

### Requirement: Deck cards view
The frontend SHALL show, within a course's deck, the list of that deck's cards (front and
back), using the paginated API. The view is reachable from the course detail's deck list.

#### Scenario: Viewing a deck's cards
- **WHEN** a user opens a deck they may read
- **THEN** the deck's cards are listed showing front and back

#### Scenario: Empty deck
- **WHEN** a deck has no cards
- **THEN** the view shows an empty state

### Requirement: Owner-only card editor
The frontend SHALL let the course owner create, edit, and delete cards in a deck. Editing
controls SHALL only appear for content the user owns; for public courses they do not own, the
cards are read-only.

#### Scenario: Owner sees the card editor
- **WHEN** an owner views a deck in their course
- **THEN** an "add card" form and per-card edit/delete controls are shown

#### Scenario: Non-owner sees read-only cards
- **WHEN** a user views a deck in a public course they do not own
- **THEN** the cards are shown without add/edit/delete controls

#### Scenario: Creating a card
- **WHEN** the owner submits the add-card form with a front and back
- **THEN** the card is created via the API and appears in the list

#### Scenario: Deleting a card asks for confirmation
- **WHEN** the owner activates delete on a card
- **THEN** the UI confirms before calling the delete endpoint

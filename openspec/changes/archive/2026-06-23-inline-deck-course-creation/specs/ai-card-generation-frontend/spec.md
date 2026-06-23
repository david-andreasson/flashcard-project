## MODIFIED Requirements

### Requirement: Save selected drafts to a deck
The screen SHALL let the user save the selected drafts as cards in one action, choosing the target
deck either by picking an existing course and deck they own, or by creating a new course and/or a
new deck inline at save time. A newly created course SHALL be private and owned by the user. Because
a new course initially has no decks, choosing to create a new course SHALL require creating a new
deck as well, and the UI SHALL NOT offer creating a new course together with an existing deck.
Saving into a newly created deck SHALL place the cards in that deck and report the result the same
way as saving into an existing deck.

#### Scenario: User saves into an existing deck
- **WHEN** the user picks an existing course and deck they own and confirms save
- **THEN** the selected drafts are created as cards in that deck and the result is shown

#### Scenario: User creates a new deck under an existing course
- **WHEN** the user picks an existing course they own, chooses to create a new deck, names it, and confirms save
- **THEN** a new deck is created under that course and the selected drafts are saved into it

#### Scenario: User creates a new course and deck
- **WHEN** the user chooses to create a new course, names both the course and the new deck, and confirms save
- **THEN** a new private course and a deck within it are created and the selected drafts are saved into that deck

#### Scenario: New course requires a new deck
- **WHEN** the user chooses to create a new course
- **THEN** the screen requires a new deck name and does not offer selecting an existing deck

#### Scenario: Retry after a partial failure does not duplicate
- **WHEN** a new course or deck was created but the card save failed, and the user retries the save
- **THEN** the previously created course and deck are reused rather than created again

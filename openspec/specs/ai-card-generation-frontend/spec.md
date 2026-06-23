# ai-card-generation-frontend Specification

## Purpose
TBD - created by archiving change ai-card-generation. Update Purpose after archive.
## Requirements
### Requirement: Generate-and-review screen
The frontend SHALL provide a screen where a permitted user pastes text, requests generation, and
sees the returned drafts listed with their front and back. The screen SHALL use the shared
authenticated request helper.

#### Scenario: User generates drafts from text
- **WHEN** a permitted user enters text and triggers generation
- **THEN** the returned drafts are displayed as an editable list

#### Scenario: Non-permitted user sees generation unavailable
- **WHEN** a FREE user attempts to generate on the screen
- **THEN** the UI surfaces that AI features require a PREMIUM plan rather than showing drafts

### Requirement: Review, edit, and select drafts before saving
The screen SHALL let the user edit a draft's front and back, remove drafts, and choose which
drafts to keep before saving. Nothing SHALL be saved until the user confirms.

#### Scenario: User edits and removes drafts
- **WHEN** the user edits a draft's text or removes a draft
- **THEN** the change is reflected in the list and no card is saved yet

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

### Requirement: Populate generation text from a PDF
The generate screen SHALL let the user upload a PDF; on success its extracted text fills the
generation text area, and the user proceeds with the existing generate → review → save flow.

#### Scenario: Uploading a PDF fills the text area
- **WHEN** the user uploads a text PDF on the generate screen
- **THEN** the extracted text appears in the generation text area, ready to generate

#### Scenario: Truncation is shown
- **WHEN** the uploaded PDF's text was truncated to the input limit
- **THEN** the screen indicates that the text was truncated

#### Scenario: A scanned or invalid PDF shows an error
- **WHEN** extraction fails (no extractable text, not a PDF, or too large)
- **THEN** the screen shows the error and the text area is left unchanged


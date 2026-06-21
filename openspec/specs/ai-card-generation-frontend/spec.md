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
The screen SHALL let the user choose a target deck they own and save the selected drafts as cards
in one action.

#### Scenario: User saves selected drafts
- **WHEN** the user picks a deck they own and confirms save
- **THEN** the selected drafts are created as cards in that deck and the result is shown


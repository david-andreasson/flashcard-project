## ADDED Requirements

### Requirement: Start studying a deck
The frontend SHALL provide a way to start studying a deck the user can read. Starting fetches
the deck's cards and presents them one at a time in shuffled order.

#### Scenario: Starting a study session
- **WHEN** a user activates "Study" on a deck that has cards
- **THEN** the cards are loaded, shuffled, and the first card's front is shown

#### Scenario: Studying an empty deck
- **WHEN** a user activates "Study" on a deck with no cards
- **THEN** the UI shows a message that there is nothing to study

### Requirement: Flip-card loop with self-grading
The study view SHALL show a card's front, let the user reveal the back, and then let the user
self-grade the card as correct or missed. Only the front SHALL be visible before the user
chooses to reveal the back.

#### Scenario: Revealing the answer
- **WHEN** a card's front is shown and the user chooses to reveal it
- **THEN** the back (and notes, if any) becomes visible

#### Scenario: Self-grading advances the session
- **WHEN** the user marks the revealed card as correct or missed
- **THEN** the next card's front is shown (or the session ends if the queue is empty)

#### Scenario: Front only before reveal
- **WHEN** a card is presented
- **THEN** its back is not visible until the user reveals it

### Requirement: Round-robin re-queue of missed cards
Cards the user marks as missed SHALL be returned to the queue and shown again later; the
session continues until every card has been answered correctly.

#### Scenario: Missed card reappears
- **WHEN** a user marks a card as missed
- **THEN** that card is shown again later in the same session

#### Scenario: Session ends when all are correct
- **WHEN** the user has answered every card correctly
- **THEN** the loop ends and a summary is shown

### Requirement: End-of-session summary and recording
At the end of a session the frontend SHALL show a summary (cards studied and how many were
correct) and record the session via the study-session API.

#### Scenario: Summary shown and session recorded
- **WHEN** a study session ends
- **THEN** a summary of total cards and correct count is shown
- **AND** the session is recorded via `POST` to the study-session API

### Requirement: Study history view
The frontend SHALL provide a view listing the user's recent study sessions with their deck and
score, behind authentication.

#### Scenario: Viewing study history
- **WHEN** a user opens their study history
- **THEN** their recent sessions are listed with deck and score, newest first

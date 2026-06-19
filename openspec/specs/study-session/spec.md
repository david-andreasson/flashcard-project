# Spec: study-session

## Purpose

Defines the StudySession domain: recording a completed study run (deck, totals, timestamp),
listing a user's own session history, and the ownership rules around it.

## Requirements

### Requirement: Record a completed study session
An authenticated user SHALL be able to record a finished study session for a deck they can
read, capturing the deck, the number of cards studied, and how many were correct. The session
is owned by the recording user.

#### Scenario: Recording a session for a readable deck
- **WHEN** a user `POST`s a finished session for a deck they own or a PUBLIC deck, with
  `totalCards` and `correctCount`
- **THEN** a `StudySession` is created owned by that user, with a finished timestamp, and
  HTTP 201 is returned

#### Scenario: Recording for an unreadable deck is rejected
- **WHEN** a user records a session for a deck in a PRIVATE course they do not own
- **THEN** the response is HTTP 404 and no session is created

#### Scenario: Invalid totals are rejected
- **WHEN** a user records a session where `correctCount` exceeds `totalCards`, or either is
  negative
- **THEN** the response is HTTP 400 and no session is created

### Requirement: List my study sessions
An authenticated user SHALL be able to list their own recent study sessions, most recent first,
paginated. A user SHALL only see their own sessions.

#### Scenario: Listing my sessions
- **WHEN** a user lists their study sessions
- **THEN** the response is a `PagedResponse` of that user's sessions, newest first

#### Scenario: Sessions are private to the user
- **WHEN** a user lists study sessions
- **THEN** sessions recorded by other users are never included

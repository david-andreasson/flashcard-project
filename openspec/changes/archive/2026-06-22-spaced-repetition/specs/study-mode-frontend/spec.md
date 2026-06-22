## ADDED Requirements

### Requirement: Spaced-repetition review mode
The study frontend SHALL offer a review mode that loads a deck's due cards from the due-queue API
and, for each, shows the front, lets the user reveal the back, and lets the user grade recall as
Again, Hard, Good, or Easy. Each grade SHALL be persisted via the review API before the next card
is shown.

#### Scenario: Grading a due card persists and advances
- **WHEN** the user reveals a due card and selects Again, Hard, Good, or Easy
- **THEN** the grade is sent to the review API and the next due card is shown

#### Scenario: Nothing due
- **WHEN** a deck has no due or new cards for the user
- **THEN** the review mode shows that nothing is due rather than presenting a card

### Requirement: Progress dashboard
The frontend SHALL provide a progress view showing the user's spaced-repetition counts — due now,
in review, and reviewed today — from the progress API, behind authentication.

#### Scenario: Viewing progress
- **WHEN** the user opens the progress view
- **THEN** their due-now, in-review, and reviewed-today counts are shown

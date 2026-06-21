# ai-card-generation Specification

## Purpose
TBD - created by archiving change ai-card-generation. Update Purpose after archive.
## Requirements
### Requirement: Generate card drafts from pasted text
An authenticated user with plan PREMIUM or role ADMIN SHALL be able to submit a block of text and
receive a list of flashcard drafts, each with a `front` and a `back`, via
`POST /api/ai/cards/generate`. The request SHALL pass through the guarded AI pipeline
(kill-switch, plan gate, input-size limit, quota, usage log) before the provider is called.

#### Scenario: Permitted user generates drafts
- **WHEN** a PREMIUM or ADMIN user posts text within the input limit and under quota
- **THEN** the response is HTTP 200 with a list of `{front, back}` drafts and the call is logged

#### Scenario: FREE user is denied generation
- **WHEN** a FREE user posts text to the generation endpoint
- **THEN** the response is HTTP 403 and no provider call or usage log occurs

#### Scenario: Oversized input is rejected
- **WHEN** a permitted user posts text larger than the configured input limit
- **THEN** the response is HTTP 400 and no provider call occurs

### Requirement: Generation does not persist cards
The generation endpoint SHALL NOT create cards, decks, or any draft records. Drafts exist only in
the response and become persistent only when explicitly saved through card creation.

#### Scenario: No cards are created by generation
- **WHEN** a permitted user generates drafts
- **THEN** no card rows are created as a result of the generation call

### Requirement: Requested draft count is bounded
The request MAY specify how many cards to generate. The system SHALL apply a configured maximum so
a single generation cannot request an unbounded number of cards.

#### Scenario: Excessive requested count is capped
- **WHEN** a permitted user requests more cards than the configured maximum
- **THEN** the system requests at most the configured maximum from the provider

### Requirement: Drafts are parsed from a structured provider response
The generation service SHALL instruct the provider to return the drafts as strict JSON and SHALL
parse that text into the list of drafts. When the provider returns content that cannot be parsed
into drafts, the system SHALL return a server-side error rather than partial or malformed drafts.

#### Scenario: Unparseable provider output is rejected
- **WHEN** the provider returns content that is not valid drafts JSON
- **THEN** the response is a server error and no drafts are returned


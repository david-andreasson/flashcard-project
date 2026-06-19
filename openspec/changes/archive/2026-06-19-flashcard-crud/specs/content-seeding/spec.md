## ADDED Requirements

### Requirement: Seed public courses with sample content
The system SHALL populate a subset of the official PUBLIC courses with sample decks and cards,
described in a JSON seed resource and owned by the configured admin. Seeding SHALL run on
startup and be idempotent.

#### Scenario: Public courses have content after seeding
- **WHEN** the application has started and the admin user exists
- **THEN** the seeded public courses contain their sample decks, and those decks contain their
  sample cards

#### Scenario: Seeding does not duplicate on restart
- **WHEN** the application starts again
- **THEN** no duplicate decks or cards are created in the seeded courses

#### Scenario: Seeding waits for the admin
- **WHEN** the application starts before any admin user exists
- **THEN** content seeding is skipped without error and runs on a later startup once the admin
  exists

### Requirement: Seed content is data, not code
Sample content SHALL be defined in a JSON resource (course title → decks → cards), so it can be
edited without changing application code.

#### Scenario: Content is read from the seed resource
- **WHEN** the seeder runs
- **THEN** the decks and cards it creates match the entries in the JSON seed resource

# Spec: course-management

## Purpose

Defines the Course domain: creation, the PUBLIC/PRIVATE visibility model, ownership-scoped
read/write rules, paginated listing, and admin-owned public course seeding.

## Requirements

### Requirement: Create a course
An authenticated user SHALL be able to create a course with a title. The course's owner is
the creating user, and its visibility defaults to PRIVATE.

#### Scenario: Successful creation
- **WHEN** an authenticated user `POST`s a valid title to `/api/courses`
- **THEN** a course is created with `owner_id` = the user, `visibility` = PRIVATE
- **AND** the response is HTTP 201 with the created course

#### Scenario: Missing title is rejected
- **WHEN** an authenticated user posts a blank or missing title
- **THEN** the response is HTTP 400 and no course is created

#### Scenario: Unauthenticated creation is rejected
- **WHEN** an unauthenticated client posts to `/api/courses`
- **THEN** the response is HTTP 401

### Requirement: Course visibility
A course SHALL have a visibility of PUBLIC or PRIVATE. PUBLIC courses are readable by every
authenticated user; PRIVATE courses are readable only by their owner. Only ADMIN users may
create or change a course to PUBLIC.

#### Scenario: Owner reads their private course
- **WHEN** the owner requests their PRIVATE course
- **THEN** the response is HTTP 200 with the course

#### Scenario: Anyone reads a public course
- **WHEN** any authenticated user requests a PUBLIC course they do not own
- **THEN** the response is HTTP 200 with the course

#### Scenario: Non-owner cannot read another user's private course
- **WHEN** a user requests a PRIVATE course owned by someone else
- **THEN** the response is HTTP 404

#### Scenario: Non-admin cannot make a course public
- **WHEN** a non-admin user attempts to set their course's visibility to PUBLIC
- **THEN** the response is HTTP 403 and the visibility is unchanged

### Requirement: List courses with pagination
The system SHALL provide paginated listing of courses. A user SHALL be able to list their own
courses and to list the available public courses. List responses SHALL use the shared
`PagedResponse` envelope.

#### Scenario: List my courses
- **WHEN** an authenticated user lists their courses with `?page=0&size=20`
- **THEN** the response contains only courses they own, paginated, in a `PagedResponse`

#### Scenario: List public courses
- **WHEN** an authenticated user lists public courses
- **THEN** the response contains only PUBLIC courses, paginated

#### Scenario: Page size is capped
- **WHEN** a user requests a page size larger than the configured maximum
- **THEN** the system returns at most the maximum page size

### Requirement: Update and delete owned courses
An authenticated user SHALL be able to update or delete a course they own. Attempting to
update or delete a course they do not own SHALL behave as if it does not exist. ADMIN users
MAY update or delete any course.

#### Scenario: Owner updates their course
- **WHEN** the owner updates the title of their course
- **THEN** the change is saved and HTTP 200 is returned

#### Scenario: Non-owner update is rejected as not found
- **WHEN** a user updates a course they do not own (and which is not theirs to write)
- **THEN** the response is HTTP 404 and nothing changes

#### Scenario: Deleting a course removes its decks
- **WHEN** the owner deletes a course that contains decks
- **THEN** the course and all its decks are removed (cascade)

#### Scenario: Admin may modify any course
- **WHEN** an ADMIN updates a course owned by another user
- **THEN** the change is saved and HTTP 200 is returned

### Requirement: Seeded public courses
The system SHALL ensure a set of official PUBLIC courses owned by the configured admin exists.
Seeding SHALL be idempotent — running it repeatedly does not create duplicates.

#### Scenario: Public courses exist after seeding
- **WHEN** the application has started and an admin user exists
- **THEN** the official PUBLIC courses are present and owned by the admin

#### Scenario: Seeding does not duplicate on restart
- **WHEN** the application starts again
- **THEN** no duplicate public courses are created

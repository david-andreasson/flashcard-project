## ADDED Requirements

### Requirement: Course browser
The frontend SHALL provide a view, behind authentication, that lists the user's own courses
and the available public courses, using the paginated API. Each course links to its detail
view.

#### Scenario: Browsing courses
- **WHEN** an authenticated user opens the courses view
- **THEN** their own courses and the public courses are listed, each linking to its detail

#### Scenario: Empty state
- **WHEN** a user has no courses of their own
- **THEN** the view shows an empty state with a way to create one (public courses still show)

### Requirement: Course detail with decks
The frontend SHALL provide a course detail view that shows the course and a paginated list of
its decks. Decks link to (future) deck content.

#### Scenario: Viewing a course
- **WHEN** a user opens a course they may read
- **THEN** the course title and its decks are shown

#### Scenario: Inaccessible course
- **WHEN** a user navigates to a course they may not read
- **THEN** the view shows a not-found message (the API returned 404)

### Requirement: Create, edit, and delete for owned content
The frontend SHALL let a user create courses, and create/edit/delete decks and edit/delete
courses they own. Edit and delete controls SHALL only be shown for content the user owns.

#### Scenario: Owner sees management controls
- **WHEN** a user views a course they own
- **THEN** edit/delete controls and an "add deck" control are visible

#### Scenario: Non-owner does not see management controls
- **WHEN** a user views a public course they do not own
- **THEN** no edit/delete/add controls are shown (read-only)

#### Scenario: Creating a course
- **WHEN** a user submits the new-course form with a title
- **THEN** the course is created via `POST /api/courses` and appears in their list

#### Scenario: Deleting a course asks for confirmation
- **WHEN** an owner activates delete on their course
- **THEN** the UI confirms before calling the delete endpoint

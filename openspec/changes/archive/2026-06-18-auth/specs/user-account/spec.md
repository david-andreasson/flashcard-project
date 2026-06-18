## ADDED Requirements

### Requirement: User registration
The system SHALL allow a new user to register with an email and password. The email MUST
be unique. The password MUST be stored only as a BCrypt hash, never in plain text. A newly
registered user SHALL default to role USER and plan FREE.

#### Scenario: Successful registration
- **WHEN** a user submits `POST /auth/register` with a unique email and a valid password
- **THEN** a new user row is created with a BCrypt password hash, role USER, and plan FREE
- **AND** the response indicates success and the user is logged in (auth cookies set)

#### Scenario: Duplicate email is rejected
- **WHEN** a user submits `POST /auth/register` with an email that already exists
- **THEN** the system responds with HTTP 409 and no new user is created

#### Scenario: Invalid input is rejected
- **WHEN** a user submits `POST /auth/register` with a malformed email or a password
  shorter than the minimum length
- **THEN** the system responds with HTTP 400 and a validation error, and no user is created

### Requirement: Password storage
The system SHALL hash all passwords with BCrypt before persistence and SHALL never return
a password hash in any API response.

#### Scenario: Password is hashed at rest
- **WHEN** a user registers
- **THEN** the stored `password_hash` is a BCrypt hash, not the raw password

#### Scenario: Hash is never exposed
- **WHEN** any user-facing endpoint returns user data
- **THEN** the response body does not contain the password hash

### Requirement: User role and plan
Each user SHALL have a role of either USER or ADMIN and a plan of either FREE or PREMIUM.
These fields SHALL be available for authorization decisions in later capabilities.

#### Scenario: Defaults on registration
- **WHEN** a user registers without any special configuration
- **THEN** their role is USER and their plan is FREE

#### Scenario: Role and plan are queryable for the current user
- **WHEN** an authenticated user calls `GET /auth/me`
- **THEN** the response includes their email, role, and plan

### Requirement: Admin bootstrap by configuration
The system SHALL grant ADMIN role to a user whose email matches the configured
`app.admin-email` value at registration time. No password or admin row SHALL be stored in
source control.

#### Scenario: Configured email becomes admin
- **WHEN** a user registers with an email equal to `app.admin-email`
- **THEN** their role is set to ADMIN instead of USER

#### Scenario: Other emails are not admin
- **WHEN** a user registers with an email different from `app.admin-email`
- **THEN** their role is USER

# Spec: authentication

## Purpose

Defines login, logout, token issuance and refresh, the JWT-cookie scheme, CSRF protection,
and the default-deny security filter chain that protects all non-public routes.

## Requirements

### Requirement: Login issues access and refresh tokens
The system SHALL authenticate a user by email and password and, on success, issue a
short-lived access token and a long-lived refresh token. Both tokens SHALL be delivered as
HttpOnly cookies. The access token SHALL carry the user's id, email, role, and plan as
signed claims.

#### Scenario: Successful login
- **WHEN** a user submits `POST /auth/login` with a correct email and password
- **THEN** the response sets an HttpOnly access-token cookie and an HttpOnly refresh-token
  cookie
- **AND** the access token contains the user's id, role, and plan claims

#### Scenario: Wrong credentials are rejected
- **WHEN** a user submits `POST /auth/login` with an incorrect password
- **THEN** the system responds with HTTP 401 and sets no auth cookies

#### Scenario: Unknown email is rejected
- **WHEN** a user submits `POST /auth/login` with an email that does not exist
- **THEN** the system responds with HTTP 401 (without revealing whether the email exists)

### Requirement: Authenticated requests via access token cookie
The system SHALL authenticate API requests by validating the access-token cookie's
signature and expiry. Validation SHALL NOT require a database lookup.

#### Scenario: Valid access token grants access
- **WHEN** a request to a protected endpoint includes a valid, unexpired access-token cookie
- **THEN** the request is processed as the authenticated user

#### Scenario: Missing or invalid token is denied
- **WHEN** a request to a protected endpoint has no access-token cookie or an invalid one
- **THEN** the system responds with HTTP 401

#### Scenario: Expired access token is denied
- **WHEN** a request to a protected endpoint includes an expired access-token cookie
- **THEN** the system responds with HTTP 401, prompting a refresh

### Requirement: Token refresh with rotation
The system SHALL issue a new access token (and a new refresh token) when presented with a
valid refresh token. Refresh tokens SHALL be stored server-side as a hash and SHALL be
rotated on each use — the previous refresh token is invalidated.

#### Scenario: Successful refresh
- **WHEN** a user calls `POST /auth/refresh` with a valid refresh-token cookie
- **THEN** the system issues a new access-token cookie and a new refresh-token cookie
- **AND** the previously used refresh token is no longer valid

#### Scenario: Invalid or revoked refresh token is denied
- **WHEN** a user calls `POST /auth/refresh` with a refresh token that is missing, expired,
  or already rotated
- **THEN** the system responds with HTTP 401 and sets no new cookies

### Requirement: Logout revokes the session
The system SHALL invalidate the user's refresh token on logout so the session cannot be
resumed, and SHALL clear the auth cookies.

#### Scenario: Logout invalidates refresh token
- **WHEN** an authenticated user calls `POST /auth/logout`
- **THEN** their refresh token is deleted from the server store
- **AND** the access-token and refresh-token cookies are cleared
- **AND** a subsequent `POST /auth/refresh` with the old refresh token returns HTTP 401

### Requirement: CSRF protection on state-changing requests
The system SHALL protect all state-changing requests (POST, PUT, PATCH, DELETE) with the
double-submit cookie pattern: a readable CSRF cookie that the client MUST echo in a request
header. Safe methods (GET, HEAD, OPTIONS) SHALL be exempt.

#### Scenario: State-changing request without CSRF header is rejected
- **WHEN** a POST/PUT/PATCH/DELETE request is made without a matching CSRF header
- **THEN** the system rejects the request (it is not processed)

#### Scenario: State-changing request with valid CSRF header succeeds
- **WHEN** a state-changing request includes a CSRF header matching the CSRF cookie
- **THEN** the request passes CSRF validation

### Requirement: Default-deny authorization
The system SHALL require authentication for every endpoint except the explicitly public
ones: `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, and
`GET /actuator/health`.

#### Scenario: Public endpoints need no auth
- **WHEN** an unauthenticated client calls a public auth endpoint or the health check
- **THEN** the request is not rejected for lack of authentication

#### Scenario: All other endpoints require auth
- **WHEN** an unauthenticated client calls any endpoint not on the public list
- **THEN** the system responds with HTTP 401

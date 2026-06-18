## MODIFIED Requirements

### Requirement: Public health check endpoint
The backend SHALL expose a `GET /actuator/health` endpoint that returns HTTP 200 and
a JSON body indicating the application and database status. This endpoint SHALL be
accessible without authentication. It is one of a small set of public endpoints; the
authentication entry points (`POST /auth/register`, `POST /auth/login`,
`POST /auth/refresh`) are also public. Every other endpoint SHALL require authentication.

#### Scenario: Health check returns 200 when healthy
- **WHEN** `GET /actuator/health` is called with no authentication
- **THEN** the response is HTTP 200 with `{"status": "UP"}` or equivalent

#### Scenario: Health check is accessible without a token
- **WHEN** `GET /actuator/health` is called without an access-token cookie
- **THEN** the response is HTTP 200 (not 401 or 403)

#### Scenario: Non-public endpoints require authentication
- **WHEN** an endpoint outside the public set is called without authentication
- **THEN** the response is HTTP 401

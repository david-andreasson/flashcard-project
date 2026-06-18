# Spec: local-infrastructure

## Purpose

Defines the local development infrastructure: Docker Compose database setup, Flyway schema migrations, environment-variable-driven configuration, and the public health check endpoint.

## Requirements

### Requirement: PostgreSQL via Docker Compose
The project SHALL provide a Docker Compose service that starts a PostgreSQL 16 database
for local development with a named volume so data persists across container restarts.

#### Scenario: Database persists between restarts
- **WHEN** a developer stops and restarts the Docker Compose stack
- **THEN** previously inserted data is still present in the database

#### Scenario: Database is reachable on the standard port
- **WHEN** the Docker Compose stack is running
- **THEN** PostgreSQL is reachable at `localhost:5432` with the configured credentials

### Requirement: Flyway manages all schema migrations
The backend SHALL use Flyway to apply and track all database schema changes. Hibernate
auto-DDL SHALL be set to `validate` — it must never create or alter tables.

#### Scenario: Application starts with a clean database
- **WHEN** the backend starts against a fresh PostgreSQL instance
- **THEN** Flyway runs all pending migrations and the application starts successfully

#### Scenario: Hibernate validates schema on startup
- **WHEN** the backend starts and the schema does not match the JPA entity mapping
- **THEN** the application fails to start with a clear validation error

### Requirement: Database configuration is environment-variable-driven
The backend SHALL read all database connection parameters from environment variables.
Local defaults SHALL be provided so the app works out-of-the-box with Docker Compose
without any manual configuration.

#### Scenario: App starts without any env vars set (local dev default)
- **WHEN** the backend starts with no datasource environment variables set
- **THEN** it connects to `localhost:5432/flashcards` using the Docker Compose defaults

#### Scenario: App uses injected env vars in a deployed environment
- **WHEN** the backend starts with `DATASOURCE_URL`, `DATASOURCE_USERNAME`, and
  `DATASOURCE_PASSWORD` set
- **THEN** it connects to the specified database instead of the local default

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

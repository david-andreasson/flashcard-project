## Why

The flashcard-project repo exists but has no code. Before any feature work can begin,
we need a working monorepo scaffold — a runnable Spring Boot backend, a runnable React
frontend, shared local infrastructure, and the conventions that all later changes will
build on.

## What Changes

- New `/backend` module: Spring Boot 3 app with PostgreSQL via Docker Compose, Flyway
  migration support, and a public `/actuator/health` endpoint (the only unprotected route).
- New `/frontend` module: React + TypeScript app built with Vite, proxied to the backend
  in development, with a minimal "app shell" (routing skeleton, no real pages yet).
- `docker-compose.yml` at repo root: PostgreSQL 16 service, environment variables for
  local dev, named volume for data persistence.
- `openspec/config.yaml` updated with project tech-stack context so future artifact
  generation stays consistent.
- CI-ready build: both modules build cleanly with `./mvnw verify` and `npm run build`.

## Capabilities

### New Capabilities

- `project-scaffold`: Monorepo structure, tooling config, and shared conventions
  (package manager, Java version, code style) that all future changes depend on.
- `local-infrastructure`: Docker Compose setup for PostgreSQL, Flyway baseline migration,
  and environment-variable-driven datasource config.
- `frontend-shell`: Vite + React + TypeScript project with React Router, a root layout
  component, and a placeholder home page — no auth yet, just the runnable shell.

### Modified Capabilities

## Impact

- No existing code is changed (repo is empty).
- Establishes Java 21 + Spring Boot 3.x as the backend baseline.
- Establishes Node 20 + Vite + React 18 + TypeScript 5 as the frontend baseline.
- PostgreSQL 16 chosen over H2 to avoid schema-quirk surprises when deploying to RDS.
- All future changes depend on this scaffold being present and buildable.

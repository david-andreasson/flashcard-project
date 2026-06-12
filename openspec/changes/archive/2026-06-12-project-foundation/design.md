## Context

The repository is empty. This change establishes the monorepo layout, tooling baselines,
and local-dev infrastructure that every subsequent change depends on. Decisions made here
(Java version, DB, build tooling) are expensive to reverse, so they are documented with
explicit rationale.

Tech stack confirmed in architecture exploration:
- Backend: Java 21 + Spring Boot 3.x
- Frontend: React 18 + TypeScript 5 + Vite
- Database: PostgreSQL 16
- Migrations: Flyway
- Auth (later): JWT in HttpOnly cookies + CSRF header
- AI (later): pluggable AiProvider interface

## Goals / Non-Goals

**Goals:**
- Both `/backend` and `/frontend` build and run locally with a single `docker compose up`
- A `GET /actuator/health` endpoint returns 200 — the only public route in the whole project
- Flyway baseline migration in place so all future schema changes are tracked migrations
- Frontend dev server proxies `/api/*` to backend (no CORS friction during development)
- Environment variables drive all config — no hardcoded credentials anywhere

**Non-Goals:**
- No authentication or authorization (that is change 02)
- No real pages or API endpoints beyond health check
- No CI/CD pipeline (that is change 10)
- No production Dockerfile (that is change 10)

## Decisions

### D1 — Java 21 (LTS) + Spring Boot 3.4.x

**Chosen**: Java 21 + Spring Boot 3.4.x  
**Alternative**: Java 17 (minimum for Boot 3.x)

Java 21 is the current LTS and is the version AWS Corretto actively targets for RDS/ECS.
Starting on 21 avoids a mid-project JDK bump. Spring Boot 3.4 is the latest GA minor.
Maven Wrapper (`./mvnw`) is included so contributors need no local Maven install.

### D2 — PostgreSQL 16 from day 1 (not H2)

**Chosen**: PostgreSQL 16 via Docker Compose  
**Alternative**: H2 in-memory for local dev, PostgreSQL only in prod

H2 supports a PostgreSQL compatibility mode, but it silently accepts SQL that PostgreSQL
rejects (e.g., certain JSON operators, generated columns, partial indexes). Discovering
schema incompatibilities at deploy time is costly. Starting with real PostgreSQL means
local dev mirrors production exactly.

Docker Compose starts a `postgres:16-alpine` container with a named volume so data
survives container restarts during development.

### D3 — Flyway for migrations (not Hibernate auto-ddl)

**Chosen**: Flyway, `spring.jpa.hibernate.ddl-auto=validate`  
**Alternative**: `spring.jpa.hibernate.ddl-auto=create-drop` (auto schema)

Auto-DDL is convenient for demos but incompatible with production deployments and
multi-developer teams. Flyway enforces an explicit migration history from the first
schema change. The baseline migration (`V1__baseline.sql`) is intentionally empty —
future changes add their own `V2__*`, `V3__*` files.

Setting `ddl-auto=validate` means Hibernate will fail fast on startup if the JPA entities
don't match the current schema, which catches migration drift early.

### D4 — Vite + React 18 + TypeScript 5

**Chosen**: Vite 5, React 18, TypeScript 5, npm  
**Alternative**: Create React App (deprecated), Next.js (SSR adds complexity not needed here)

Vite is the current standard for SPA tooling: fast HMR, native ESM, actively maintained.
CRA is deprecated. Next.js is overkill for an authenticated single-page app with a
separate Spring Boot backend.

TypeScript strict mode enabled from day 1 — adding it later requires fixing the whole
codebase at once.

### D5 — Vite dev proxy for `/api`

**Chosen**: `vite.config.ts` proxies `/api` → `http://localhost:8080`  
**Alternative**: Configure CORS on the backend

CORS config adds boilerplate and must be kept in sync with the frontend origin. The Vite
proxy makes the frontend treat the backend as same-origin during development, which also
reflects how the app will work in production (same domain, reverse proxy). CORS config
will still be needed for production but can be added at deployment time.

### D6 — All config via environment variables

**Chosen**: `application.yml` reads from env vars with local defaults  
**Alternative**: Separate `application-local.yml` profiles

A single `application.yml` with `${DATASOURCE_URL:jdbc:postgresql://localhost:5432/flashcards}`
pattern works for both local (defaults) and deployed (env var overrides). No profile
switching needed. Secrets never appear in committed files.

### D7 — Spring Boot context path `/api`

**Chosen**: `server.servlet.context-path=/api`  
**Alternative**: No context path; Vite proxy rewrites `/api/xxx` → `/xxx`

Setting a context path means every Spring Boot route is implicitly under `/api` without
any per-controller annotation. The Vite dev proxy forwards `/api/*` to the backend
unchanged, which matches production behaviour (a reverse proxy or ALB routes `/api/*`
to the backend, `/` to the frontend CDN). Path-rewrite proxies add a hidden layer that
differs between dev and prod, and must be replicated in every environment config.

Practical effect: a controller annotated `@GetMapping("/courses")` is reached at
`/api/courses` both locally and in production — no prefix needed in the annotation itself.

Discovered during implementation: a local PostgreSQL instance was already bound to
`0.0.0.0:5432`, causing the Docker-mapped port to be intercepted. The Docker Compose
port mapping was changed to `5433:5432`; the application default JDBC URL uses `5433`.
No impact on production — RDS will use a dedicated hostname, not `localhost`.

## Risks / Trade-offs

- [Docker required for local dev] → Acceptable for this project; Docker Desktop is
  standard. Document in README.
- [Vite proxy only works in dev] → Production will use a reverse proxy (Nginx or
  CloudFront + ALB). The abstraction is the same, just configured differently.
- [Empty Flyway baseline] → If a developer runs the app before any real schema migration
  is added, the DB will be empty but Flyway will be happy. First real migration (change 02,
  User table) will be V2.

## Open Questions

- None — all decisions confirmed during architecture exploration.

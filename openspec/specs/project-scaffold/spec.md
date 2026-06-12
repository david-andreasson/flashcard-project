# Spec: project-scaffold

## Purpose

Defines the repository layout, toolchain version pinning, and secrets hygiene for the monorepo. Establishes the structural baseline that all other capabilities build on.

## Requirements

### Requirement: Monorepo directory layout
The repository SHALL contain a `/backend` directory holding the Maven project and a
`/frontend` directory holding the npm project. A `docker-compose.yml` SHALL exist at
the repository root.

#### Scenario: Backend module is present and buildable
- **WHEN** a developer runs `./mvnw verify` from `/backend`
- **THEN** the build succeeds with no compilation errors and no failing tests

#### Scenario: Frontend module is present and buildable
- **WHEN** a developer runs `npm run build` from `/frontend`
- **THEN** the build produces a `dist/` directory with no errors

#### Scenario: Root docker-compose brings up services
- **WHEN** a developer runs `docker compose up -d` from the repository root
- **THEN** the PostgreSQL service starts and is reachable on port 5432

### Requirement: Java and Node version pinning
The project SHALL pin exact toolchain versions so all contributors use the same baseline.

#### Scenario: Java version is declared
- **WHEN** a developer opens `/backend/pom.xml`
- **THEN** the `java.version` property is set to `21`

#### Scenario: Node version is declared
- **WHEN** a developer opens `/frontend/.nvmrc` or `package.json`
- **THEN** the Node version requirement is `>=20`

### Requirement: No secrets in committed files
The project SHALL not contain hardcoded passwords, API keys, or other secrets in any
committed file.

#### Scenario: Database password is not hardcoded
- **WHEN** a developer searches all committed files for the local dev database password
- **THEN** passwords appear only in `.env.example` (as placeholders) or are read from
  environment variables at runtime

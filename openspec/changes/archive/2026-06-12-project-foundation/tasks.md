## 1. Repository scaffold

- [x] 1.1 Create `/backend` and `/frontend` directories at the repository root
- [x] 1.2 Generate Spring Boot 3.4.x project in `/backend` via Spring Initializr with dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway Core, Spring Boot Actuator, Lombok, Spring Security (placeholder config)
- [x] 1.3 Confirm `pom.xml` sets `<java.version>21</java.version>` and Maven Wrapper (`./mvnw`) is present
- [x] 1.4 Add `/frontend/.nvmrc` with `20` and scaffold Vite + React + TypeScript project via `npm create vite@latest`
- [x] 1.5 Enable TypeScript strict mode in `/frontend/tsconfig.json`
- [x] 1.6 Add a root `.gitignore` covering `/backend/target`, `/frontend/node_modules`, `/frontend/dist`, `.env`

## 2. Local infrastructure (Docker Compose + PostgreSQL)

- [x] 2.1 Create `docker-compose.yml` at the repo root with a `postgres:16-alpine` service, named volume `pgdata`, and env vars `POSTGRES_DB=flashcards`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- [x] 2.2 Create `.env.example` at the repo root with placeholder values for all Docker Compose env vars; add `.env` to `.gitignore`
- [x] 2.3 Configure `/backend/src/main/resources/application.yml` to read datasource URL, username, and password from env vars with local-dev defaults matching Docker Compose
- [x] 2.4 Set `spring.jpa.hibernate.ddl-auto=validate` in `application.yml`
- [x] 2.5 Create the Flyway baseline migration at `/backend/src/main/resources/db/migration/V1__baseline.sql` (empty file — establishes migration history)
- [x] 2.6 Verify backend starts cleanly against the Docker Compose database (`./mvnw spring-boot:run`)

## 3. Health check endpoint

- [x] 3.1 Add `management.endpoints.web.exposure.include=health` to `application.yml`
- [x] 3.2 Add a temporary Spring Security config that permits `GET /actuator/health` and denies all other requests (full security config comes in change 02)
- [x] 3.3 Confirm `GET http://localhost:8080/actuator/health` returns HTTP 200 with `{"status":"UP"}`

## 4. Frontend shell

- [x] 4.1 Install React Router v6 (`npm install react-router-dom`)
- [x] 4.2 Create a `RootLayout` component with a navigation placeholder and `<Outlet />`
- [x] 4.3 Create a `HomePage` placeholder component rendered at route `/`
- [x] 4.4 Create a `NotFoundPage` component used as the catch-all route
- [x] 4.5 Wire up `createBrowserRouter` in `main.tsx` with `RootLayout` wrapping `HomePage` and `NotFoundPage`
- [x] 4.6 Configure Vite dev proxy in `vite.config.ts`: proxy `/api` → `http://localhost:8080`
- [x] 4.7 Add `VITE_API_BASE_URL` support in a shared `src/lib/api.ts` helper (defaults to `""` so relative paths go through the proxy)
- [x] 4.8 Add `.env.example` in `/frontend` with `VITE_API_BASE_URL=` (empty by default)

## 5. Verification

- [x] 5.1 Run `docker compose up -d` and confirm PostgreSQL starts and data volume is created
- [x] 5.2 Run `./mvnw verify` from `/backend` — build must pass with no errors
- [x] 5.3 Run `npm run build` from `/frontend` — build must produce `dist/index.html`
- [x] 5.4 Start both backend and frontend dev server; open browser and confirm root layout renders without console errors
- [x] 5.5 Confirm `GET /actuator/health` returns 200 from the browser via the Vite proxy (`fetch('/api/actuator/health')`)
- [x] 5.6 Navigate to `/unknown-path` and confirm the NotFoundPage renders

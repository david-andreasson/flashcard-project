# Spec: frontend-shell

## Purpose

Defines the frontend application shell: the Vite + React + TypeScript project structure, React Router root layout, development proxy configuration, and environment variable handling for the backend base URL.

## Requirements

### Requirement: Vite + React + TypeScript project
The frontend SHALL be a Vite-based React 18 application written in TypeScript with
strict mode enabled. The project SHALL build to a `dist/` directory of static assets.

#### Scenario: TypeScript strict mode is enabled
- **WHEN** a developer opens `/frontend/tsconfig.json`
- **THEN** `"strict": true` is present under `compilerOptions`

#### Scenario: Production build succeeds
- **WHEN** `npm run build` is run from `/frontend`
- **THEN** the command exits with code 0 and `dist/index.html` exists

### Requirement: React Router root layout
The frontend SHALL include React Router v6 with a root layout component that wraps all
routes. The layout SHALL include a top-level navigation placeholder and an `<Outlet />`
for nested routes. A single placeholder home page route SHALL exist at `/`.

#### Scenario: Application renders without errors
- **WHEN** a developer runs `npm run dev` and opens `http://localhost:5173`
- **THEN** the browser renders the root layout and home placeholder without console errors

#### Scenario: Unknown routes show a fallback
- **WHEN** a user navigates to a path that has no matching route
- **THEN** the app renders a "page not found" message (not a blank screen)

### Requirement: Development proxy for backend API
The Vite dev server SHALL proxy all requests to `/api/*` to the backend at
`http://localhost:8080`. This SHALL require no CORS configuration in the backend
during development.

#### Scenario: API requests reach the backend from the frontend
- **WHEN** the frontend dev server and backend are both running locally
- **THEN** a `fetch('/api/actuator/health')` from the browser resolves to the backend
  response without CORS errors

### Requirement: Environment variable for backend base URL
The frontend SHALL read the backend base URL from a `VITE_API_BASE_URL` environment
variable so it can be overridden at build time for different deployment targets.

#### Scenario: Default base URL is empty (uses proxy in dev)
- **WHEN** `VITE_API_BASE_URL` is not set
- **THEN** API calls use relative paths (e.g., `/api/...`), which the Vite proxy intercepts

#### Scenario: Base URL can be overridden for production
- **WHEN** `VITE_API_BASE_URL=https://api.example.com` is set at build time
- **THEN** API calls are prefixed with that URL

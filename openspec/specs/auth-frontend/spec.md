# Spec: auth-frontend

## Purpose

Defines the React authentication UI: registration and login pages, the auth context that
tracks the current user, route guarding, the CSRF header on mutations, and the logout action.

## Requirements

### Requirement: Registration and login pages
The frontend SHALL provide a registration page and a login page. Each SHALL collect the
required fields, submit to the corresponding `/api/auth` endpoint, and show validation and
error messages returned by the backend.

#### Scenario: User registers from the UI
- **WHEN** a user fills in the registration form with a valid email and password and submits
- **THEN** the frontend calls `POST /api/auth/register` and, on success, the user is
  authenticated and redirected to the home page

#### Scenario: User logs in from the UI
- **WHEN** a user submits the login form with correct credentials
- **THEN** the frontend calls `POST /api/auth/login` and, on success, redirects to the home page

#### Scenario: Backend errors are shown
- **WHEN** the backend returns a 400/401/409 error
- **THEN** the form displays a clear message (e.g. "Email already in use", "Invalid
  credentials") without leaving the page blank

### Requirement: Authentication context and current user
The frontend SHALL maintain an auth context that tracks whether a user is authenticated and
holds the current user's profile (email, role, plan), populated from `GET /api/auth/me`.

#### Scenario: Current user loaded on app start
- **WHEN** the app loads and a valid session exists
- **THEN** `GET /api/auth/me` populates the auth context with the user's email, role, and plan

#### Scenario: Context cleared on logout
- **WHEN** the user logs out
- **THEN** the auth context is cleared and the user is treated as unauthenticated

### Requirement: Route guarding
The frontend SHALL redirect unauthenticated users away from protected routes to the login
page, and SHALL keep public routes (login, register) accessible without a session.

#### Scenario: Protected route redirects when not authenticated
- **WHEN** an unauthenticated user navigates to a protected route
- **THEN** they are redirected to the login page

#### Scenario: Authenticated user reaches protected route
- **WHEN** an authenticated user navigates to a protected route
- **THEN** the route renders normally

### Requirement: CSRF header on state-changing requests
The frontend API helper SHALL read the CSRF cookie and attach its value as the CSRF request
header on all state-changing requests.

#### Scenario: Mutating request includes CSRF header
- **WHEN** the frontend issues a POST/PUT/PATCH/DELETE to the API
- **THEN** the request includes the CSRF header matching the CSRF cookie

### Requirement: Logout action
The frontend SHALL provide a logout control that calls `POST /api/auth/logout`, clears the
auth context, and redirects to the login page.

#### Scenario: User logs out
- **WHEN** the user activates the logout control
- **THEN** the frontend calls `POST /api/auth/logout`, clears local auth state, and shows the
  login page

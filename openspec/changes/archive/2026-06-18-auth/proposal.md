## Why

The app currently denies every request except the health check (placeholder security
from change 01). Before any real feature can exist, users need to register, log in, and
have their identity carried securely across requests. This change replaces the placeholder
with a complete authentication system and establishes the role/plan model that all later
authorization (course ownership, AI plan-gating) depends on.

## What Changes

- **BREAKING**: The placeholder `SecurityConfig` (deny-all, CSRF disabled) is replaced
  with a JWT cookie + CSRF security filter chain.
- New `User` entity and `users` table: email, BCrypt password hash, role (USER/ADMIN),
  plan (FREE/PREMIUM), timestamps.
- New `RefreshToken` entity and `refresh_tokens` table: server-side store of long-lived
  refresh tokens (stored hashed) so logout and revocation actually work.
- New public endpoints (no auth required): `POST /auth/register`, `POST /auth/login`,
  `POST /auth/refresh`, `POST /auth/logout`.
- New authenticated endpoint: `GET /auth/me` returns the current user's profile.
- Access token (short-lived, ~15 min) and refresh token (long-lived, ~7 days) both
  delivered as HttpOnly, Secure, SameSite cookies.
- CSRF protection via the double-submit cookie pattern (cookie + matching request header)
  on all state-changing requests.
- Config-driven admin bootstrap: the email in `app.admin-email` is granted ADMIN on
  registration.
- Frontend: registration page, login page, logout action, and an auth context that
  gates routes and reads the current user from `GET /auth/me`.

## Capabilities

### New Capabilities

- `user-account`: The User domain — registration, the users table, password hashing,
  role and plan fields, and the admin bootstrap rule.
- `authentication`: Login, logout, token issuance/refresh, the JWT cookie scheme, CSRF
  protection, and the security filter chain that protects all non-public routes.
- `auth-frontend`: React registration/login pages, logout, the auth context/provider,
  and route guarding that redirects unauthenticated users to login.

### Modified Capabilities

- `local-infrastructure`: The "Public health check endpoint" requirement is extended —
  health check is no longer the *only* public route; the `/auth/*` entry points are also
  public. The default-deny posture for everything else is now enforced by real auth.

## Impact

- Replaces `backend/src/main/java/com/flashcard/config/SecurityConfig.java`.
- Adds Flyway migration `V2__users_and_refresh_tokens.sql` (first real schema).
- Adds Spring dependencies: `spring-boot-starter-validation`, `jjwt` (or equivalent JWT lib).
- Adds `app.admin-email`, `app.jwt.*` config keys to `application.yml`.
- Establishes the authentication pattern every later change relies on (ownership checks in
  change 03, plan-gating in change 06 both read role/plan set up here).
- Frontend gains its first real API integration and protected-route pattern.

## Context

Change 01 left a deny-all placeholder `SecurityConfig` with CSRF disabled and only
`/actuator/health` public. This change builds the real authentication system on top of
the existing Spring Boot + PostgreSQL + Flyway stack. The `/api` context path means all
auth routes are reached at `/api/auth/*`.

The authentication model was settled during exploration:
- JWT delivered in HttpOnly cookies + CSRF header (decided in change 01 architecture phase)
- Stateful refresh tokens (chosen during change-02 exploration) so logout truly revokes
- BCrypt password hashing, open registration, config-driven admin bootstrap

This change also establishes the `role` (USER/ADMIN) and `plan` (FREE/PREMIUM) fields that
later changes depend on: ownership checks (change 03) and AI plan-gating (change 06).

## Goals / Non-Goals

**Goals:**
- Users can register, log in, refresh their session, and log out
- All routes except `/auth/*` and `/actuator/health` require authentication by default
- Logout and revocation actually invalidate a session (stateful refresh tokens)
- Tokens are never readable by JavaScript (HttpOnly cookies) and state-changing requests
  are CSRF-protected
- Role and plan are carried in the access token so authorization needs no DB lookup
- The configured admin email is granted ADMIN automatically

**Non-Goals:**
- Email verification, password reset, "forgot password" flows (future change if needed)
- OAuth / social login
- Rate limiting on login attempts (worth a future hardening change; noted as a risk)
- Multi-factor authentication
- Billing / how a user becomes PREMIUM (change 06+ territory) — plan defaults to FREE

## Decisions

### D1 — Access + refresh token model (stateful)

**Chosen**: Short-lived access token (~15 min) + long-lived refresh token (~7 days), with
refresh tokens stored server-side in `refresh_tokens`.
**Alternative**: Single stateless access token, no server state.

Stateless is simpler but cannot revoke a token before expiry, so "logout" would be
cosmetic. Storing refresh tokens lets logout and password-change revoke real sessions, and
enables future "log out all devices". The access token stays stateless (validated by
signature only — no DB hit on normal requests), so the common path is still fast. Only
`/auth/refresh` and `/auth/logout` touch the token table.

### D2 — Both tokens in HttpOnly cookies

**Chosen**: `access_token` and `refresh_token` cookies, both `HttpOnly; Secure;
SameSite=Strict` (Lax acceptable if a cross-site redirect flow is ever needed).
**Alternative**: Access token in memory (JS), refresh in cookie.

Keeping both out of JavaScript reach eliminates token theft via XSS. The refresh cookie is
scoped to the `/api/auth/refresh` path so it is only sent when needed. `Secure` is enforced
in production; for local HTTP dev a profile flag relaxes it.

### D3 — CSRF via double-submit cookie pattern

**Chosen**: Spring Security `CookieCsrfTokenRepository` with a non-HttpOnly `XSRF-TOKEN`
cookie; the frontend echoes it in an `X-XSRF-TOKEN` header on state-changing requests.
**Alternative**: Disable CSRF (only safe with non-cookie token transport).

Because auth tokens live in cookies, the browser auto-attaches them to every request,
which reopens CSRF risk. The double-submit pattern closes it: an attacker's cross-site
request carries the cookie but cannot read it to set the matching header. `GET` requests
are exempt; `POST/PUT/PATCH/DELETE` require the header.

### D4 — BCrypt password hashing

**Chosen**: Spring Security `BCryptPasswordEncoder` (default strength 10).
**Alternative**: Argon2.

BCrypt is the Spring default, well-understood, needs no extra dependency, and is more than
adequate here. Argon2 is stronger but adds a dependency and tuning burden not warranted for
a learning project. The `PasswordEncoder` bean is an interface, so swapping later only
re-hashes on next login.

### D5 — Refresh tokens stored hashed, with rotation

**Chosen**: Store a SHA-256 hash of the refresh token, not the raw value. On each
`/auth/refresh`, delete (or mark used) the old token and issue a new one (rotation).
**Alternative**: Store raw tokens; no rotation.

If the DB leaks, raw refresh tokens are live sessions — hashing makes a leak useless.
Rotation limits the window a stolen refresh token is valid and enables reuse-detection
later. The raw token only ever exists in the user's cookie.

### D6 — Config-driven admin bootstrap

**Chosen**: `app.admin-email` config key; when a user registers with that email they are
assigned ADMIN. All other users default to USER.
**Alternative**: Seed an admin row in a migration; manual SQL promotion.

A seed migration would bake a password hash into version control. The config approach keeps
the credential out of the repo (the admin just registers normally) and is easy to change
per environment. For local dev the default is the project owner's email.

### D7 — Token claims carry role and plan

**Chosen**: Access token claims = `sub` (user id), `email`, `role`, `plan`, `iat`, `exp`.
**Alternative**: Claims carry only `sub`; load role/plan from DB each request.

Putting `role` and `plan` in the signed token lets authorization decisions (route guards,
plan-gating in change 06) happen with zero DB lookups. The trade-off: a role/plan change
only takes effect when the access token next refreshes (≤15 min). That staleness window is
acceptable; for immediate effect a user's refresh token can be revoked.

## Risks / Trade-offs

- [Stale role/plan in access token until refresh] → Window is ≤15 min; revoke refresh
  token to force re-auth sooner. Acceptable for this app.
- [No login rate limiting] → Brute-force is possible. Out of scope here; flag a future
  hardening change. BCrypt's cost slows attempts somewhat.
- [SameSite=Strict may break future cross-site embeds] → Not a concern for a standalone
  SPA; revisit only if embedding is ever needed.
- [Local HTTP dev vs Secure cookies] → A dev profile relaxes the `Secure` flag so cookies
  work over `http://localhost`. Production always sets `Secure`.
- [Clock skew on JWT exp] → Use a small leeway (e.g. 30s) when validating expiry.

## Migration Plan

- `V2__users_and_refresh_tokens.sql` creates both tables. This is the first real migration
  (V1 was an empty baseline), so there is no existing data to migrate.
- Rollback: drop both tables; revert `SecurityConfig`. Since there is no production data
  yet, rollback is low-risk.

## Open Questions

- None blocking. Token lifetimes (15 min / 7 days) are sensible defaults and live in config,
  so they can be tuned without code changes.

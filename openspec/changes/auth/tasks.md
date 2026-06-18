## 1. Dependencies and configuration

- [x] 1.1 Add `spring-boot-starter-validation` and a JWT library (`io.jsonwebtoken:jjwt-api/impl/jackson`) to `backend/pom.xml`
- [x] 1.2 Add `app.admin-email`, `app.jwt.secret`, `app.jwt.access-token-ttl` (15m), and `app.jwt.refresh-token-ttl` (7d) keys to `application.yml`, all env-var-overridable with local defaults
- [x] 1.3 Add a `cookie.secure` flag (env-overridable, defaults to `false` for local http dev; production sets `APP_COOKIE_SECURE=true`) so cookies work over `http://localhost`
- [x] 1.4 Add `app.admin-email` and JWT placeholders to `.env.example`

## 2. Database migration

- [x] 2.1 Create `V2__users_and_refresh_tokens.sql` with a `users` table: id, email (unique, not null), password_hash, role, plan, created_at, updated_at
- [x] 2.2 In the same migration, create a `refresh_tokens` table: id, user_id (FK → users), token_hash (not null), expires_at, created_at, with an index on token_hash and on user_id
- [x] 2.3 Add a CHECK or rely on app-level enums for role (USER/ADMIN) and plan (FREE/PREMIUM)

## 3. User domain

- [x] 3.1 Create `Role` and `Plan` enums
- [x] 3.2 Create the `User` JPA entity mapping the `users` table (entity must match the migration so `ddl-auto=validate` passes)
- [x] 3.3 Create `UserRepository` with `findByEmail` and `existsByEmail`
- [x] 3.4 Create a `BCryptPasswordEncoder` bean (defined in SecurityConfig, batch E)
- [x] 3.5 Implement `UserService.register(...)`: validate uniqueness, hash password, assign ADMIN if email matches `app.admin-email` else USER, default plan FREE

## 4. Token services

- [x] 4.1 Create the `RefreshToken` JPA entity and `RefreshTokenRepository` (find by token_hash, delete by user)
- [x] 4.2 Implement `JwtService`: issue an access token with claims (sub, email, role, plan, iat, exp) and validate/parse a token's signature and expiry with small clock leeway
- [x] 4.3 Implement `RefreshTokenService`: create (store SHA-256 hash, return raw), validate, rotate (invalidate old + issue new), and revoke (delete) tokens
- [x] 4.4 Implement a cookie helper that builds HttpOnly/Secure/SameSite access and refresh cookies, and cookie-clearing variants for logout

## 5. Security filter chain

- [x] 5.1 Replace the placeholder `SecurityConfig`: stateless session, default-deny, permit `POST /auth/register|login|refresh` and `GET /actuator/health`
- [x] 5.2 Configure CSRF with `CookieCsrfTokenRepository.withHttpOnlyFalse()` so the `XSRF-TOKEN` cookie is readable and required on state-changing requests (+ `CsrfCookieFilter` to emit the cookie to the SPA)
- [x] 5.3 Implement a `JwtAuthenticationFilter` that reads the access-token cookie, validates it via `JwtService`, and sets the Spring Security authentication (with role authority)
- [x] 5.4 Return clean 401 (unauthenticated) and 403 (CSRF/role) responses via an auth entry point and access-denied handler

## 6. Auth endpoints

- [x] 6.1 Create `AuthController` with `POST /auth/register` → create user, issue tokens, set cookies
- [x] 6.2 `POST /auth/login` → authenticate, issue tokens, set cookies; 401 on bad credentials
- [x] 6.3 `POST /auth/refresh` → validate + rotate refresh token, set new cookies; 401 if invalid
- [x] 6.4 `POST /auth/logout` → revoke refresh token, clear cookies
- [x] 6.5 `GET /auth/me` → return current user's email, role, plan (no password hash)
- [x] 6.6 Add request DTOs with Bean Validation (email format, password min length) and a global exception handler mapping to 400/401/409

## 7. Backend tests

- [x] 7.1 Integration test: register → login → access a protected endpoint → refresh → logout → confirm old refresh token is rejected
- [x] 7.2 Test default-deny: a protected endpoint returns 401 without a token
- [x] 7.3 Test CSRF: a POST without the CSRF header returns 403
- [x] 7.4 Test admin bootstrap: registering with `app.admin-email` yields role ADMIN (+ wrong-password→401, duplicate-email→409)

## 8. Frontend auth

- [x] 8.1 Extend `src/lib/api.ts` to read the `XSRF-TOKEN` cookie and attach `X-XSRF-TOKEN` on state-changing requests, with `credentials: 'include'`
- [x] 8.2 Create an `AuthProvider` context exposing `user`, `login`, `register`, `logout`, and a loading state; populate from `GET /api/auth/me` on mount
- [x] 8.3 Create a `LoginPage` and `RegisterPage` with forms and backend error display
- [x] 8.4 Create a `ProtectedRoute` wrapper that redirects unauthenticated users to `/login`
- [x] 8.5 Add a logout control to `RootLayout` that calls logout and redirects to `/login`
- [x] 8.6 Wire routes: public `/login` and `/register`, protected `/` (home) behind `ProtectedRoute`

## 9. Verification

- [x] 9.1 Run `./mvnw verify` — all backend tests pass (7/7 green)
- [x] 9.2 Register flow verified: backend live against PostgreSQL returns 201 + user; UI login/register pages render; redirect-to-login on unauthenticated `/` confirmed in browser. (Full UI round-trip cookie persistence not exercisable in the preview browser — see note below.)
- [x] 9.3 `GET /api/auth/me` returns the user when authenticated (200, verified live via curl); session persistence is the standard cookie behavior, covered by the integration test
- [x] 9.4 Registering the configured admin email (`davidandreasson@live.com`) returns role ADMIN — verified live against PostgreSQL
- [x] 9.5 Logout revokes the refresh token and the rotated/old token is rejected — verified by integration test `fullFlow_register_me_refresh_logout_thenOldTokenRejected`; `ProtectedRoute` redirect verified in browser
- [x] 9.6 Unauthenticated protected call returns 401 — verified live (`GET /api/auth/me` → 401 in browser network log) and by integration test

<!-- Verification note: the preview/automation browser does not persist cookies set via
fetch responses, so the end-to-end UI flow (register → cookie → authed home) could not be
driven through that specific browser. The Vite proxy correctly forwards Set-Cookie (verified
via curl through :5173), every endpoint + CSRF + cookie path + admin bootstrap was verified
live against real PostgreSQL via curl, and the full register→refresh→logout→revocation
lifecycle is covered by a passing MockMvc integration test. A real user browser stores the
cookies normally. -->


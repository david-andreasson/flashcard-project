## 1. Dependencies and configuration

- [ ] 1.1 Add `spring-boot-starter-validation` and a JWT library (`io.jsonwebtoken:jjwt-api/impl/jackson`) to `backend/pom.xml`
- [ ] 1.2 Add `app.admin-email`, `app.jwt.secret`, `app.jwt.access-token-ttl` (15m), and `app.jwt.refresh-token-ttl` (7d) keys to `application.yml`, all env-var-overridable with local defaults
- [ ] 1.3 Add a `cookie.secure` flag (default `true`, relaxed to `false` via a `local` profile) so cookies work over `http://localhost` in dev
- [ ] 1.4 Add `app.admin-email` and JWT placeholders to `.env.example`

## 2. Database migration

- [ ] 2.1 Create `V2__users_and_refresh_tokens.sql` with a `users` table: id, email (unique, not null), password_hash, role, plan, created_at, updated_at
- [ ] 2.2 In the same migration, create a `refresh_tokens` table: id, user_id (FK → users), token_hash (not null), expires_at, created_at, with an index on token_hash and on user_id
- [ ] 2.3 Add a CHECK or rely on app-level enums for role (USER/ADMIN) and plan (FREE/PREMIUM)

## 3. User domain

- [ ] 3.1 Create `Role` and `Plan` enums
- [ ] 3.2 Create the `User` JPA entity mapping the `users` table (entity must match the migration so `ddl-auto=validate` passes)
- [ ] 3.3 Create `UserRepository` with `findByEmail` and `existsByEmail`
- [ ] 3.4 Create a `BCryptPasswordEncoder` bean
- [ ] 3.5 Implement `UserService.register(...)`: validate uniqueness, hash password, assign ADMIN if email matches `app.admin-email` else USER, default plan FREE

## 4. Token services

- [ ] 4.1 Create the `RefreshToken` JPA entity and `RefreshTokenRepository` (find by token_hash, delete by user)
- [ ] 4.2 Implement `JwtService`: issue an access token with claims (sub, email, role, plan, iat, exp) and validate/parse a token's signature and expiry with small clock leeway
- [ ] 4.3 Implement `RefreshTokenService`: create (store SHA-256 hash, return raw), validate, rotate (invalidate old + issue new), and revoke (delete) tokens
- [ ] 4.4 Implement a cookie helper that builds HttpOnly/Secure/SameSite access and refresh cookies, and cookie-clearing variants for logout

## 5. Security filter chain

- [ ] 5.1 Replace the placeholder `SecurityConfig`: stateless session, default-deny, permit `POST /auth/register|login|refresh` and `GET /actuator/health`
- [ ] 5.2 Configure CSRF with `CookieCsrfTokenRepository.withHttpOnlyFalse()` so the `XSRF-TOKEN` cookie is readable and required on state-changing requests
- [ ] 5.3 Implement a `JwtAuthenticationFilter` that reads the access-token cookie, validates it via `JwtService`, and sets the Spring Security authentication (with role authority)
- [ ] 5.4 Return clean 401 (unauthenticated) and 403 (CSRF/role) responses via an auth entry point and access-denied handler

## 6. Auth endpoints

- [ ] 6.1 Create `AuthController` with `POST /auth/register` → create user, issue tokens, set cookies
- [ ] 6.2 `POST /auth/login` → authenticate, issue tokens, set cookies; 401 on bad credentials
- [ ] 6.3 `POST /auth/refresh` → validate + rotate refresh token, set new cookies; 401 if invalid
- [ ] 6.4 `POST /auth/logout` → revoke refresh token, clear cookies
- [ ] 6.5 `GET /auth/me` → return current user's email, role, plan (no password hash)
- [ ] 6.6 Add request DTOs with Bean Validation (email format, password min length) and a global exception handler mapping to 400/401/409

## 7. Backend tests

- [ ] 7.1 Integration test: register → login → access a protected endpoint → refresh → logout → confirm old refresh token is rejected
- [ ] 7.2 Test default-deny: a protected endpoint returns 401 without a token
- [ ] 7.3 Test CSRF: a POST without the CSRF header returns 403
- [ ] 7.4 Test admin bootstrap: registering with `app.admin-email` yields role ADMIN

## 8. Frontend auth

- [ ] 8.1 Extend `src/lib/api.ts` to read the `XSRF-TOKEN` cookie and attach `X-XSRF-TOKEN` on state-changing requests, with `credentials: 'include'`
- [ ] 8.2 Create an `AuthProvider` context exposing `user`, `login`, `register`, `logout`, and a loading state; populate from `GET /api/auth/me` on mount
- [ ] 8.3 Create a `LoginPage` and `RegisterPage` with forms and backend error display
- [ ] 8.4 Create a `ProtectedRoute` wrapper that redirects unauthenticated users to `/login`
- [ ] 8.5 Add a logout control to `RootLayout` that calls logout and redirects to `/login`
- [ ] 8.6 Wire routes: public `/login` and `/register`, protected `/` (home) behind `ProtectedRoute`

## 9. Verification

- [ ] 9.1 Run `./mvnw verify` — all backend tests pass
- [ ] 9.2 Start backend + frontend; register a new user via the UI and confirm redirect to home
- [ ] 9.3 Confirm `GET /api/auth/me` returns the user; reload the page and confirm the session persists
- [ ] 9.4 Register with the configured admin email and confirm role is ADMIN in `GET /api/auth/me`
- [ ] 9.5 Log out and confirm protected routes redirect to `/login` and the old session cannot refresh
- [ ] 9.6 Confirm an unauthenticated `GET /api/courses`-style protected call returns 401 (use any protected path)

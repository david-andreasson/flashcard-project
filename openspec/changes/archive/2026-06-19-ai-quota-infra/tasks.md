## 1. Configuration

- [x] 1.1 Add `ai.*` config to `application.yml`: `ai.enabled` (true), `ai.provider` (mock), `ai.max-input-chars`, `ai.default-max-tokens`, `ai.pricing.input-rate` + `ai.pricing.output-rate` (USD per token), and per-plan monthly token limits (`ai.quota.free`=0, `ai.quota.premium`, `ai.quota.admin` — large/unlimited)
- [x] 1.2 Bind the keys via a typed `AiProperties` (`@ConfigurationProperties("ai")`); add env-overridable defaults and `.env.example` entries

## 2. Provider abstraction

- [x] 2.1 Create `AiRequest` (systemPrompt, userMessage, maxTokens) and `AiResponse` (content, inputTokens, outputTokens, modelId) records
- [x] 2.2 Create the `AiProvider` interface (`AiResponse complete(AiRequest)`)
- [x] 2.3 Implement `MockAiProvider` (deterministic content, derived token counts; no external call), active via `@ConditionalOnProperty(name="ai.provider", havingValue="mock", matchIfMissing=true)`

## 3. Usage logging

- [x] 3.1 Create `V6__ai_usage_log.sql`: `ai_usage_log` table (id, user_id FK → users ON DELETE CASCADE, feature_key, input_tokens, output_tokens, estimated_cost_usd NUMERIC, created_at), index on `(user_id, created_at)`
- [x] 3.2 Create `AiUsageLog` JPA entity matching the migration (so `ddl-auto=validate` passes) and `AiUsageLogRepository`
- [x] 3.3 Add a repository query summing `inputTokens + outputTokens` for a user since a given instant (month start)

## 4. Quota + access pipeline

- [x] 4.1 Implement `AiQuotaService`: month-to-date token usage for a user (calendar month, UTC) and the plan's configured limit; `isOverQuota(principal)`
- [x] 4.2 Add exceptions for the new HTTP semantics: quota exceeded → 429, kill-switch/AI-disabled → 503 (reuse `ForbiddenException` 403 and `BadRequestException` 400); map them in `CommonExceptionHandler`
- [x] 4.3 Implement `AiService.complete(principal, featureKey, request)` running the pipeline in order: kill-switch (503) → plan gate PREMIUM/ADMIN (403) → input-size limit (400) → quota (429) → `provider.complete()` → write `AiUsageLog` (tokens + estimated cost); apply `default-max-tokens` when the request omits it
- [x] 4.4 Return the provider's `AiResponse` (or a thin result) from `AiService` for callers to use

## 5. Demo endpoint

- [x] 5.1 Create `AiEchoController` (`POST /ai/echo`): body = `{ message }`; calls `AiService.complete(principal, "echo", ...)`; returns echoed content + token usage. Validates message not blank
- [x] 5.2 Ensure the endpoint is authenticated (existing security) and exercises the full pipeline (no provider bypass)

## 6. Backend tests

- [x] 6.1 Kill-switch: with `ai.enabled=false`, `/ai/echo` → 503 and no usage logged
- [x] 6.2 Plan gate: FREE user → 403; PREMIUM → 200; ADMIN → 200
- [x] 6.3 Input limit: oversized message → 400
- [x] 6.4 Quota: seed usage at the PREMIUM limit → next call 429; under limit → 200 and usage grows
- [x] 6.5 Logging: a successful call writes an `AiUsageLog` row with feature key, token counts, and estimated cost
- [x] 6.6 Monthly summation: usage from a previous month is excluded from the current month's total

## 7. Verification

- [x] 7.1 Run `./mvnw verify` — all backend tests pass (29/29 green)
- [x] 7.2 Backend booted against PostgreSQL: `V6` migration applied and `ddl-auto=validate` passed
- [x] 7.3 ADMIN hit `/ai/echo` → 200 with echoed content + token usage (in=12, out=6); an `AiUsageLog` row was written to PostgreSQL with the correct estimated cost (0.000126) — verified live via curl + psql
- [x] 7.4 FREE user → 403 (plan gate), blank message → 400 — verified live. Kill-switch (`ai.enabled=false` → 503) is covered by `AiKillSwitchTest` (a pure config toggle; not re-demoed live)
- [x] 7.5 Quota → 429 is covered by `AiPipelineIntegrationTest.quota_overLimit_429` (seeds usage to the limit, asserts 429). Not demoed live: there is no API path yet to make a user PREMIUM, and it is a config-driven check fully exercised by the test

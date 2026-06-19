## Why

AI features (card generation in change 07, PDF import in 09) call a paid external model, so
they need cost protection *before* they exist: plan-gating, input limits, monthly quotas, and
usage logging. This change builds that shared infrastructure once — a single guarded pipeline
every AI feature routes through — so no feature can reach the model without passing the gates.
It ships with a mock provider so the whole thing is testable without an API key or any cost;
the real Anthropic provider arrives with the first feature in change 07.

## What Changes

- New **`AiProvider`** interface with `AiRequest` (system prompt, user message, maxTokens) and
  `AiResponse` (content, input/output token counts, model id). A **`MockAiProvider`** is the
  default; the real Anthropic implementation is deferred to change 07.
- New **`AiUsageLog`** entity/table recording every AI call: user, feature key, input/output
  tokens, estimated cost, timestamp.
- New **`AiService`** pipeline that every AI call goes through, in order: kill-switch → plan
  gate (PREMIUM or ADMIN) → input-size limit → monthly token quota → `provider.complete()` →
  log usage.
- New **`AiQuotaService`** computing a user's current calendar-month token usage and comparing
  it to their plan's configured limit.
- **Plan-gating** lives in `AiService` (service guard) — FREE users are rejected with 403.
- **Cost-protection config**: `ai.enabled` kill-switch, `ai.provider` selector, per-plan
  monthly token limits, max input size, default output `maxTokens`, and token price rates for
  cost estimation.
- A small, gated **`POST /ai/echo`** demo endpoint (backed by the mock provider) so the gates
  are observable end-to-end: FREE → 403, PREMIUM → 200, over-quota → 429. This is scaffolding
  the first real feature (change 07) builds on.

## Capabilities

### New Capabilities

- `ai-provider`: The provider abstraction — `AiProvider` interface, request/response model with
  token accounting, config-based selection, and the mock implementation.
- `ai-usage-logging`: The `AiUsageLog` record of every call and the queries that sum a user's
  monthly token usage.
- `ai-access-control`: The guarded `AiService` pipeline — kill-switch, plan-gating, input
  limits, monthly token quotas, and the resulting HTTP semantics (403 / 400 / 429 / 503).

### Modified Capabilities

## Impact

- Adds Flyway migration `V6__ai_usage_log.sql` (ai_usage_log table, FK to users, indexes for
  per-user monthly summation).
- Adds `ai.*` config keys to `application.yml` (kill-switch, provider, per-plan limits, caps,
  price rates), all env-overridable.
- Reads the current user's `plan`/`role` from `AuthPrincipal` (change 02) for gating.
- Reuses the `common` exception handler; adds 429 (quota) and 503 (kill-switch) mappings.
- Establishes the pipeline that change 07 (`ai-card-generation`) and change 09 (`pdf-import`)
  plug into — they add a provider call site and a `featureKey`, nothing more.
- Does NOT call the real Claude API or require an API key — the Anthropic provider and the
  first real feature are change 07.

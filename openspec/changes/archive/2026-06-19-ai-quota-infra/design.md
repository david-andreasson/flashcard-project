## Context

This change builds the cost-protection infrastructure that every AI feature routes through,
before any AI feature exists. The User model already carries `plan` (FREE/PREMIUM) and `role`
(USER/ADMIN), available via `AuthPrincipal`. Decisions were settled in exploration: ship a
**mock provider + demo endpoint** now (real Anthropic in change 07), put **plan-gating in the
service** alongside the quota check, and meter **tokens per user per calendar month**.

## Goals / Non-Goals

**Goals:**
- One guarded pipeline (`AiService`) every AI feature must call — impossible to reach the model
  without passing kill-switch, plan gate, input limit, and quota
- Pluggable provider abstraction with token accounting; mock implementation for free testing
- Per-user monthly token usage logging and quota enforcement, all configurable
- A demo endpoint proving the gates work end-to-end

**Non-Goals:**
- The real Anthropic/Claude provider and any real model call — change 07 (needs an API key,
  model ids, live testing)
- An actual user-facing AI feature (card generation is change 07)
- Per-feature or per-request billing UI, payment, plan upgrades
- A global cross-user budget cap (possible later hardening; per-user quota + kill-switch suffice)
- Streaming responses (the abstraction returns a complete `AiResponse`)

## Decisions

### D1 — `AiProvider` abstraction with token accounting

**Chosen**: `interface AiProvider { AiResponse complete(AiRequest request); }` where
`AiRequest` = (systemPrompt, userMessage, maxTokens) and `AiResponse` = (content, inputTokens,
outputTokens, modelId). Selected by `@ConditionalOnProperty(ai.provider)`.
**Alternative**: Call an SDK directly from each feature.

A narrow interface keeps providers swappable and, crucially, puts token counts in the response
so usage logging and quotas are provider-agnostic. The mock returns deterministic content and
plausible token counts; the real Anthropic provider (change 07) maps the SDK response into the
same shape.

### D2 — `MockAiProvider` is the default; real Anthropic deferred to change 07

**Chosen**: Ship only `MockAiProvider` here (deterministic echo-style output, derived token
counts). `ai.provider=mock` is the default. The Anthropic provider is change 07.
**Alternative**: Build the Anthropic provider now.

Nothing in this change needs a real model, and requiring an API key + live API testing for pure
infrastructure adds cost and flakiness. The mock makes the whole pipeline unit/integration
testable for free. Change 07 adds `AnthropicAiProvider` and flips `ai.provider=anthropic`.

### D3 — The `AiService` pipeline (one guarded path)

**Chosen**: A single `AiService.complete(principal, featureKey, request)` that runs, in order:
1. **Kill-switch** — `ai.enabled=false` → 503
2. **Plan gate** — `plan == PREMIUM || role == ADMIN`, else 403
3. **Input limit** — userMessage length over `ai.max-input-chars` → 400
4. **Quota** — month-to-date tokens >= plan limit → 429
5. `provider.complete(request)`
6. **Log** an `AiUsageLog` row (tokens + estimated cost)

**Alternative**: Scatter these checks across controllers/filters.

Centralizing every check in one method means a feature literally cannot call the model without
them. Features (07, 09) call `AiService.complete(...)` with their own `featureKey`; they never
touch the provider, quota, or log directly.

### D4 — Plan-gating in the service, not Spring Security

**Chosen**: The plan gate is step 2 of the `AiService` pipeline (throws `ForbiddenException`).
**Alternative**: Grant an "AI" authority in `JwtAuthenticationFilter` and gate `/ai/**` in
`SecurityConfig`.

The quota check must live in the service (it reads the DB), so keeping the plan gate there too
keeps all AI-access logic in one cohesive place rather than split between the security config
and the service. Spring Security still protects the endpoints generally (authentication); the
*AI-specific* gate is the service's job.

### D5 — Quota: tokens per user per calendar month

**Chosen**: Sum `inputTokens + outputTokens` from `ai_usage_log` for the current user where
`created_at >= start-of-calendar-month (UTC)`; compare to the plan's configured monthly limit.
Over the limit → 429.
**Alternative**: Rolling 30-day window, or call-count or cost-based quotas.

A calendar-month token budget is easy to reason about and resets predictably on the 1st. The
pre-call check is necessarily approximate — output tokens are only known after the call — so a
user already at/over the limit is rejected, and the per-call `maxTokens` cap bounds how far a
tipping call can overshoot. Per-plan limits live in config (FREE=0 since FREE is gated out
anyway; PREMIUM=a sane default; ADMIN effectively unlimited).

### D6 — Cost estimation from config rates

**Chosen**: `estimatedCostUsd = inputTokens * inputRate + outputTokens * outputRate`, with rates
in `ai.pricing.*` config. Stored on each `AiUsageLog` row.
**Alternative**: Hardcode per-model prices, or omit cost.

Config-driven rates avoid baking provider prices into code and make the estimate adjustable.
Exact Anthropic model ids and prices are pulled into config when the real provider is wired
(change 07).

### D7 — `POST /ai/echo` demo endpoint

**Chosen**: A small endpoint that runs the full `AiService` pipeline with the mock provider and
returns the echoed content + token usage. It makes the gates observable (403/400/429/503) and
is the call-site pattern change 07 copies.
**Alternative**: No endpoint; tests only.

A live demo endpoint de-risks change 07 (the pipeline is proven through a real HTTP path) and
lets us *see* the protection work. It is intentionally thin scaffolding; change 07 adds the real
feature endpoint(s).

## Risks / Trade-offs

- [Quota overshoot on the tipping call] → Accepted; bounded by per-call `maxTokens`. Real
  enforcement precision isn't needed for a personal app.
- [Mock provider gives false confidence] → The pipeline, gating, quota math, and logging are all
  real and tested; only the model call is mocked. Change 07 swaps in the real provider behind the
  same interface, so the integration surface is tiny.
- [No global budget cap] → A determined PREMIUM/ADMIN user could spend within their own quota;
  the kill-switch is the blunt global control. A cross-user cap can be added later if needed.
- [Calendar-month boundary in UTC vs local] → Use UTC consistently; acceptable for a study app.

## Open Questions

- Exact PREMIUM monthly token limit and price rates are config values, not architecture — they
  get sensible defaults here and real numbers when the Anthropic provider lands (change 07).

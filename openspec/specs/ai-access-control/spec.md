# Spec: ai-access-control

## Purpose

Defines the guarded AiService pipeline: global kill-switch, plan-gating, input-size limit, and
monthly token quotas, together with the HTTP semantics (503 / 403 / 400 / 429).

## Requirements

### Requirement: Guarded AI pipeline
The system SHALL route every AI call through a single guarded pipeline that, in order, applies:
a global kill-switch, a plan gate, an input-size limit, and a monthly token quota, before
calling the provider and logging usage. No AI feature SHALL reach the provider without passing
all gates.

#### Scenario: A permitted call passes all gates and is logged
- **WHEN** AI is enabled, a PREMIUM user submits an input within limits and is under quota
- **THEN** the provider is called and the usage is logged

### Requirement: Global kill-switch
When AI is disabled by configuration (`ai.enabled=false`), the system SHALL reject all AI calls
with HTTP 503 and SHALL NOT call any provider.

#### Scenario: Kill-switch blocks all AI
- **WHEN** `ai.enabled` is false and any user makes an AI call
- **THEN** the response is HTTP 503 and no provider call or usage log occurs

### Requirement: Plan-gated AI access
AI features SHALL be available only to users with plan PREMIUM or role ADMIN. FREE users (USER
role) SHALL be rejected with HTTP 403.

#### Scenario: FREE user is denied
- **WHEN** a FREE user makes an AI call
- **THEN** the response is HTTP 403 and no provider call or usage log occurs

#### Scenario: PREMIUM user is allowed
- **WHEN** a PREMIUM user makes an AI call within other limits
- **THEN** the call proceeds

#### Scenario: ADMIN is allowed regardless of plan
- **WHEN** an ADMIN user makes an AI call within other limits
- **THEN** the call proceeds

### Requirement: Input-size limit
The system SHALL reject AI requests whose input exceeds the configured maximum size with HTTP
400, before calling the provider.

#### Scenario: Oversized input is rejected
- **WHEN** a permitted user submits an input larger than `ai.max-input-chars`
- **THEN** the response is HTTP 400 and no provider call occurs

### Requirement: Monthly token quota
The system SHALL reject AI calls from a user whose current calendar-month token usage has
reached their plan's configured limit, with HTTP 429. Per-plan limits SHALL be configurable.

#### Scenario: Over-quota user is rejected
- **WHEN** a permitted user's month-to-date tokens have reached their plan limit
- **THEN** a further AI call returns HTTP 429 and no provider call occurs

#### Scenario: Under-quota user proceeds
- **WHEN** a permitted user is below their plan's monthly token limit
- **THEN** the call proceeds and the resulting tokens are added to their usage

### Requirement: Output token cap
Each AI request SHALL carry a maximum output token count, defaulting to a configured value, so a
single call cannot produce unbounded output.

#### Scenario: Default output cap is applied
- **WHEN** a request does not specify a max output token count
- **THEN** the configured default `maxTokens` is used

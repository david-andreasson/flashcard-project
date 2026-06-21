## Context

Change 06 built the guarded AI pipeline (`AiService`: kill-switch → plan gate → input limit →
quota → provider → usage log) and a deterministic `MockAiProvider`, but no real provider and no
real feature exist. This change adds both: a 1min.ai-backed provider and a card-generation feature
that runs through the existing pipeline.

1min.ai is a model aggregator: one API key and one credit pool give access to many underlying
models (Claude, GPT, Gemini, …). The confirmed contract (from the public API reference and a
community relay):

- `POST https://api.1min.ai/api/chat-with-ai`, JSON body
  `{ "type": "UNIFY_CHAT_WITH_AI", "model": "<model>", "promptObject": { "prompt": "<text>" } }`
- Response: `{ "aiRecord": { "aiRecordDetail": { "resultObject": ["<generated text>"] } } }`
- The response carries generated text only — **no token counts**. 1min.ai meters in credits.

The `AiProvider` abstraction from change 06 was designed for exactly this: a new provider is a new
implementation selected by `ai.provider`, with no change to calling code.

## Goals / Non-Goals

**Goals:**
- A real `OneMinAiProvider` selected by `ai.provider=1min`; the mock stays the default.
- The first AI feature: paste text → drafts → review → save, running through the guarded pipeline.
- Keep the provider abstraction generic and provider-neutral so future providers (Anthropic
  direct, others) slot in without touching features.

**Non-Goals:**
- A direct Anthropic provider — deferred to a later change; the seam makes it additive.
- A self-serve plan-upgrade path. Only ADMIN (or a user whose plan is PREMIUM in the database) can
  use AI today; closing that gap is tracked separately, not here.
- Persisting drafts or a draft-lifecycle state machine.
- Streaming responses.

## Decisions

### 1. Structured output via prompt + JSON parsing, not a provider-specific structured-output API
The card service instructs the model to return a strict JSON array of `{front, back}` and parses
the returned text with Jackson. `AiProvider` stays generic (text in, text out).
- *Why:* provider-neutral. 1min.ai returns plain text; an Anthropic-SDK structured-output feature
  would not port to it. One parsing path works for every provider.
- *Alternative:* Anthropic's `output_config.format`. Rejected — it couples the feature to one SDK
  and defeats the point of the abstraction.

### 2. 1min.ai via Spring `RestClient`, not an SDK
`OneMinAiProvider` calls `POST {base-url}/api/chat-with-ai` with `RestClient`, sends
`systemPrompt + "\n\n" + userMessage` as `promptObject.prompt` (type `UNIFY_CHAT_WITH_AI`), and
reads `aiRecord.aiRecordDetail.resultObject`.
- *Why:* 1min.ai has no Java SDK; the contract is a single POST. `RestClient` is already available
  in Spring Boot 3.4.
- *Auth/model:* API key and model come from provider-scoped config (`ai.onemin.*`), default model
  Claude Haiku. The exact auth header (`API-KEY` vs `Authorization: Bearer`) and current Haiku slug
  are confirmed at implementation time (see Open Questions).

### 3. Shared `TokenEstimator`; quota runs on estimates; cost is an approximation
Because 1min.ai reports no usage, the provider estimates input/output tokens from text (~4 chars
per token), reusing the logic currently inside `MockAiProvider`. That logic is extracted to a
shared `TokenEstimator` used by both the mock and 1min.ai providers.
- *Why:* the change-06 quota and logging pipeline is token-based and must keep working unchanged.
- *Trade-off:* `estimatedCostUsd` becomes a rough internal proxy, not 1min.ai's real credit charge.
  The quota (the actual guardrail) is what protects spend; the USD figure is indicative.

### 4. Stateless drafts — no persistence, no new table
`POST /ai/cards/generate` returns drafts in the response body. Review/edit/select happens in the
browser. Nothing is stored until the user saves.
- *Why:* simplest thing that works; no draft state machine, no migration.
- *Alternative:* a `card_draft` table with PENDING/ACCEPTED states. Rejected as over-scoped for the
  value it adds here.

### 5. Saving is plain bulk card creation, outside the AI guard
`POST /api/courses/{courseId}/decks/{deckId}/cards/bulk` creates the selected drafts as cards in one
ownership-checked request. It does not touch `AiService` and consumes no quota.
- *Why:* no model runs at save time, so the AI cost guard does not belong there. The seam is clean:
  cost protection wraps only generation.

### 6. Thread the feature key to the provider so the mock stays end-to-end usable
The feature key (e.g. `card-generation`) is carried on the request the provider receives, so
`MockAiProvider` can return canned drafts JSON for the card-generation feature.
- *Why:* preserves the change-06 property that the mock exercises the **whole** pipeline locally
  with no API key — including the card-generation happy path.
- *Alternative:* leave the mock generic and stub the provider with `@MockBean` only in the
  happy-path test. Rejected — generation would then fail locally under the default mock, and the
  mock would no longer model a real feature's output.

### 7. Upstream and parse failures → HTTP 502
A failed 1min.ai call, or output that cannot be parsed into drafts, surfaces as 502 via a new
`UpstreamAiException`. The change-06 kill-switch keeps its 503.
- *Why:* 502 (bad gateway) distinguishes "our upstream model service failed" from "the feature is
  turned off" (503). Usage is logged only after a successful provider call (existing pipeline
  order), so a transport failure writes no usage row; a post-call parse failure does (the tokens
  were spent).

## Risks / Trade-offs

- **1min.ai contract confirmed from community sources, not first-party deep docs** → isolate every
  1min.ai detail inside `OneMinAiProvider`; verify auth header and request shape against the public
  Postman collection during implementation; unit-test the provider against a stubbed HTTP response.
- **Estimated tokens ≠ real credit spend** → quota is approximate but conservative and configurable;
  it is a guardrail, not an invoice.
- **Model returns malformed JSON** → strip code fences before parsing and fail with 502 if still
  unparseable; no partial drafts are returned. No automatic retry (keeps cost predictable).
- **Output size on 1min.ai** → 1min.ai's `promptObject` may not accept a max-output-tokens
  parameter, so the change-06 output cap may not be enforceable upstream; the requested-count
  instruction and the input limit bound the work instead.
- **Secret handling** → `ONEMIN_API_KEY` is read from the environment, never committed; the mock
  stays the default so CI and a fresh checkout need no key.

## Migration Plan

- No database migration (drafts are not persisted; saved cards use the existing `cards` table).
- Enable the real provider per environment by setting `AI_PROVIDER=1min` and `ONEMIN_API_KEY`.
  With neither set, the mock provider remains active and every existing test passes unchanged.
- Rollback: set `AI_PROVIDER=mock` (or `AI_ENABLED=false`) — no redeploy of code required.

## Open Questions

- Exact 1min.ai auth header: `API-KEY: <key>` vs `Authorization: Bearer <key>` — confirm against
  the Postman collection.
- ~~Current Claude Haiku model identifier on 1min.ai~~ — resolved: `claude-haiku-4-5-20251001`
  (Claude Haiku 4.5), confirmed against the maintained `llm-1minai` plugin's model registry.
  Overridable per environment via `ONEMIN_MODEL`.
- Whether `/api/features` accepts a per-request output-token cap; if not, output is bounded only by
  the requested count and the input limit.

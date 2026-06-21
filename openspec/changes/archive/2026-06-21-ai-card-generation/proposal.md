## Why

Change 06 built the guarded AI pipeline (kill-switch, plan gate, input limit, quota, usage log)
and a mock provider, but there is no real AI feature and no real provider. This change delivers
the first user-facing AI feature — generating flashcard drafts from pasted text — and the first
real provider, exercising the change-06 abstraction end to end.

## What Changes

- Add a real `AiProvider` backed by the 1min.ai HTTP API, selected with `ai.provider=1min`. The
  mock provider stays the default. Provider configuration becomes provider-scoped (base URL, API
  key, model); the default model via 1min.ai is Claude Haiku.
- 1min.ai returns only generated text and no token counts. Providers that do not report usage
  SHALL estimate input and output tokens; the estimation already used by the mock is extracted to
  a shared helper so the quota and cost pipeline keep working unchanged.
- Add an AI card-generation feature: paste text → guarded pipeline → provider → a list of
  `{front, back}` card drafts returned to the caller. Generation persists nothing; the model is
  instructed to return strict JSON which the service parses.
- Add bulk card creation in a deck so a reviewed set of drafts is saved in one request. Saving is
  plain card creation (ownership-checked, no AI cost) — the AI guard wraps only generation.
- Add a frontend screen: paste text, generate, review/edit/select drafts, choose a target deck,
  and save.

## Capabilities

### New Capabilities
- `ai-card-generation`: generate flashcard drafts from pasted text through the guarded AI
  pipeline (PREMIUM or ADMIN), returning drafts for review without persisting them.
- `ai-card-generation-frontend`: the paste → generate → review → save user interface.

### Modified Capabilities
- `ai-provider`: add a real external HTTP provider (1min.ai) and a requirement that providers
  which do not report token usage estimate it; provider-scoped model and credential config.
- `flashcard-management`: add bulk creation of cards in a deck.

## Impact

- Backend: new `OneMinAiProvider` (Spring `RestClient`), shared `TokenEstimator`,
  `AiCardGenerationService` + controller (`POST /ai/cards/generate`), a bulk endpoint on the
  cards controller, and `ai.provider` / `ai.onemin.*` configuration in `application.yml`.
- Secrets: `ONEMIN_API_KEY` environment variable (no key committed; mock remains default).
- Frontend: a generate → review → save screen using the existing `apiFetch` helper.
- No database migration: drafts are not persisted; saved cards use the existing `cards` table.

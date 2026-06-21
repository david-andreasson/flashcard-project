## 1. Provider seam (shared estimation, feature key, config)

- [x] 1.1 Extract the token estimate (~4 chars/token, min 1 for non-empty) from `MockAiProvider` into a shared `TokenEstimator`; update `MockAiProvider` to use it.
- [x] 1.2 Carry the feature key to the provider: add it to the request the provider receives and have `AiService` set it when building the effective request; leave `AiService` logging behaviour unchanged.
- [x] 1.3 Make `MockAiProvider` return canned `{front,back}` drafts JSON for the `card-generation` feature key, and keep `[mock] <user>` for other features.
- [x] 1.4 Add provider-scoped config `ai.onemin` (base-url, api-key, model with a Claude Haiku default) bound as typed properties; wire `ONEMIN_API_KEY` in `application.yml`; note `1min` in the provider doc comment.

## 2. 1min.ai provider

- [x] 2.1 Add `UpstreamAiException` (HTTP 502) in `common` and map it in `CommonExceptionHandler`.
- [x] 2.2 Implement `OneMinAiProvider` (`@ConditionalOnProperty ai.provider=1min`) with `RestClient`: POST `{base-url}/api/features` with `type=CHAT_WITH_AI`, configured `model`, and `promptObject.prompt = systemPrompt + "\n\n" + userMessage`; read `aiRecord.aiRecordDetail.resultObject[0]`; estimate tokens via `TokenEstimator`; return `AiResponse(content, in, out, model)`.
- [x] 2.3 Throw `UpstreamAiException` on non-2xx, empty, or unreadable responses (no usage row is written, since logging follows a successful provider call).
- [x] 2.4 Confirm the auth header (`API-KEY` vs `Authorization: Bearer`) and the current Haiku model slug against the 1min.ai Postman collection; set the header and default model accordingly.

## 3. Card-generation feature

- [x] 3.1 DTOs: `GenerateCardsRequest(@NotBlank text, Integer count)`, `CardDraft(front, back)`, `GenerateCardsResponse(List<CardDraft> drafts, usage)`.
- [x] 3.2 Add `ai.card-generation.max-count` config (default 30) and size the generation `maxTokens` to the requested count.
- [x] 3.3 `AiCardGenerationService`: build a system prompt instructing a strict JSON array of `{front,back}` (≤ max count); cap the requested count; call `aiService.complete(principal, "card-generation", request)`; parse `response.content()` (stripping code fences) into `List<CardDraft>`; throw `UpstreamAiException` when unparseable.
- [x] 3.4 `AiCardGenerationController`: `POST /ai/cards/generate` returning drafts + usage (pipeline gates handled by `AiService`).

## 4. Bulk card creation (the save step)

- [x] 4.1 DTO `BulkCreateCardsRequest(@NotEmpty @Valid List<CreateCardRequest> cards)` with a bounded size.
- [x] 4.2 `CardService.createBulk(courseId, deckId, principal, items)`: resolve `getWritable` + deck-in-course once, then `saveAll`; bean validation rejects an invalid batch before persistence.
- [x] 4.3 `CardController`: `POST /courses/{courseId}/decks/{deckId}/cards/bulk` → HTTP 201 with the created cards.

## 5. Frontend (generate → review → save)

- [x] 5.1 API client: `generateCards(text, count)` and `bulkCreateCards(courseId, deckId, cards)` via the shared `apiFetch` helper.
- [x] 5.2 Generate-and-review screen: text area + generate button → editable drafts list (edit front/back, remove, select).
- [x] 5.3 Target-deck picker (user's decks) + save action calling `bulkCreateCards`; surface 403 (PREMIUM required) and 502 (generation failed) states.
- [x] 5.4 Add the route and a navigation entry to the screen.

## 6. Backend tests

- [x] 6.1 `TokenEstimator` unit test (empty → 0; non-empty → positive, ~chars/4).
- [x] 6.2 `OneMinAiProvider` unit test against a stubbed HTTP response: maps `resultObject[0]`, estimates tokens, and throws `UpstreamAiException` on error/empty.
- [x] 6.3 Card-generation gate tests (MockMvc, mock provider): FREE → 403, oversized input → 400, over-quota → 429, kill-switch → 503; assert no usage row where the gate precedes the provider.
- [x] 6.4 Card-generation happy path (mock returns canned drafts): 200 with drafts, exactly one usage row logged, and no cards created; unparseable output → 502.
- [x] 6.5 Bulk-create tests: owner → 201 with all cards created; non-owner → 404 with none created; a blank front/back item → 400 with none created.

## 7. Config, verification, docs

- [x] 7.1 `application.yml`: keep `ai.provider` default `mock`; add `ai.onemin.{base-url,api-key,model}` and `ai.card-generation.max-count`.
- [x] 7.2 Manual smoke test with `AI_PROVIDER=1min` + `ONEMIN_API_KEY`: generate from a sample text and bulk-save into a deck; record the request/response.
- [x] 7.3 Update `docs/ARCHITECTURE.md` (provider seam, 1min.ai contract, generate→review→save flow) and mark change 07 on the roadmap.
- [x] 7.4 Run `./mvnw test` and the frontend build; confirm all green with the default mock provider (no API key required).

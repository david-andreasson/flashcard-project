## ADDED Requirements

### Requirement: Real external provider via 1min.ai
The system SHALL provide an `AiProvider` implementation backed by the 1min.ai HTTP API, selected
when `ai.provider` is `1min`. It SHALL send the request's system prompt and user message to the
1min.ai chat feature as a single text prompt and map the returned text into the standard response
shape. Calling code SHALL NOT change when this provider is active.

#### Scenario: 1min.ai provider is selected by configuration
- **WHEN** the application starts with `ai.provider=1min`
- **THEN** the 1min.ai provider is the active `AiProvider` and AI calls are routed to it without
  any change to calling code

#### Scenario: Provider maps a 1min.ai response into the standard shape
- **WHEN** the 1min.ai provider receives a successful response containing generated text
- **THEN** it returns a response whose content is that text, with input and output token counts
  and a model identifier

### Requirement: Token estimation for providers that do not report usage
A provider whose backend does not return token counts SHALL estimate the input and output token
counts from the request and response text, so the quota and usage-logging pipeline operates
unchanged. The estimation SHALL be shared between such providers, including the mock.

#### Scenario: A provider without usage data estimates tokens
- **WHEN** an active provider's backend returns generated text but no token counts
- **THEN** the response carries non-negative estimated input and output token counts derived from
  the prompt and the generated text

### Requirement: Provider-scoped configuration
A provider that calls an external service SHALL read its base URL, API key, and model from
provider-scoped configuration, so the model and credentials are chosen per provider and no secret
is hard-coded. The 1min.ai provider SHALL default to a Claude Haiku model.

#### Scenario: External provider reads its credentials and model from configuration
- **WHEN** the 1min.ai provider is active
- **THEN** it uses the configured base URL, API key, and model, defaulting to a Claude Haiku model
  when none is overridden

### Requirement: Upstream provider failures do not record usage
When an external provider's backend call fails or returns an unusable response, the system SHALL
NOT write a usage-log row for that call and SHALL surface a server-side error.

#### Scenario: Upstream failure is not logged
- **WHEN** the active external provider's backend returns an error or is unreachable
- **THEN** no usage row is written for that call and the caller receives a server error

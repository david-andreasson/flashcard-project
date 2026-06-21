# Spec: ai-provider

## Purpose

Defines the AI provider abstraction: the AiProvider interface, the request/response model with
token accounting, config-based selection, and the mock implementation.
## Requirements
### Requirement: AI provider abstraction
The system SHALL define an `AiProvider` abstraction that takes an AI request (system prompt,
user message, max output tokens) and returns a response containing the generated content and
the input and output token counts and the model identifier. AI features SHALL depend only on
this abstraction, never on a specific provider SDK.

#### Scenario: A provider returns content and token usage
- **WHEN** the system calls the active `AiProvider` with a request
- **THEN** the response includes the generated content, the input token count, the output token
  count, and the model id

### Requirement: Configurable provider selection
The active provider SHALL be chosen by configuration (`ai.provider`). The default provider
SHALL be a mock that requires no external service or API key.

#### Scenario: Mock provider is active by default
- **WHEN** the application starts with no `ai.provider` override
- **THEN** the mock provider is active and AI calls succeed without any external API key

#### Scenario: Provider can be switched by configuration
- **WHEN** `ai.provider` is set to a different registered provider
- **THEN** that provider is used instead, without changes to calling code

### Requirement: Mock provider is deterministic and free
The mock provider SHALL return deterministic content and plausible token counts without calling
any external service, so the AI pipeline is fully testable at no cost.

#### Scenario: Mock call produces token counts
- **WHEN** the mock provider handles a request
- **THEN** it returns content and non-negative input and output token counts derived from the
  request and response

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


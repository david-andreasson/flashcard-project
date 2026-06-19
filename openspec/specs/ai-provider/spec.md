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

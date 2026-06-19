## ADDED Requirements

### Requirement: Log every AI call
The system SHALL record every completed AI call in an `AiUsageLog`, capturing the user, a
feature key identifying the calling feature, the input and output token counts, an estimated
cost, and a timestamp.

#### Scenario: A successful AI call is logged
- **WHEN** an AI call completes through the pipeline
- **THEN** an `AiUsageLog` row is created for the calling user with the feature key, token
  counts, estimated cost, and the current time

#### Scenario: Estimated cost is derived from token counts
- **WHEN** a usage row is written
- **THEN** its estimated cost equals `inputTokens * inputRate + outputTokens * outputRate` using
  the configured price rates

### Requirement: Sum a user's monthly token usage
The system SHALL be able to compute a user's total tokens used in the current calendar month
(input plus output) from the usage log, for quota evaluation.

#### Scenario: Monthly usage counts only this month
- **WHEN** a user's month-to-date usage is computed
- **THEN** only usage rows from the current calendar month are summed; earlier months are
  excluded

#### Scenario: No usage yields zero
- **WHEN** a user has no usage rows this month
- **THEN** their month-to-date token usage is zero

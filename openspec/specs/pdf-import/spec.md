# pdf-import Specification

## Purpose
TBD - created by archiving change pdf-import. Update Purpose after archive.
## Requirements
### Requirement: Extract text from an uploaded PDF
An authenticated user SHALL be able to upload a PDF and receive its extracted text, via
`POST /api/ai/cards/extract-pdf` as multipart form data. The response SHALL include the extracted
text, the page count, the character count, and whether the text was truncated.

#### Scenario: A text PDF is extracted
- **WHEN** an authenticated user uploads a PDF that contains embedded text
- **THEN** the response is HTTP 200 with the extracted text and its page and character counts

### Requirement: Extracted text is capped at the AI input limit
The extracted text SHALL be truncated to the configured AI input limit (`ai.max-input-chars`) so it
can be fed to generation, and the response SHALL indicate whether truncation occurred.

#### Scenario: A long PDF is truncated
- **WHEN** the extracted text exceeds the configured input limit
- **THEN** the returned text is truncated to that limit and the response marks it as truncated

#### Scenario: A short PDF is not truncated
- **WHEN** the extracted text is within the input limit
- **THEN** the full text is returned and the response marks it as not truncated

### Requirement: PDFs without extractable text are rejected
When an uploaded PDF contains no extractable text (for example a scanned image), the system SHALL
reject it with a clear client error rather than returning empty text. OCR is not provided.

#### Scenario: A scanned PDF is rejected
- **WHEN** a user uploads a PDF with no embedded text
- **THEN** the response is a client error stating that no text could be extracted

### Requirement: Upload validation
The system SHALL reject a non-PDF file, an unreadable or corrupt PDF, and a file exceeding the
configured size limit.

#### Scenario: Non-PDF upload is rejected
- **WHEN** a user uploads a file that is not a PDF
- **THEN** the response is HTTP 400

#### Scenario: Corrupt PDF is rejected
- **WHEN** a user uploads a file that cannot be parsed as a PDF
- **THEN** the response is HTTP 400 with a clear error

#### Scenario: Oversized upload is rejected
- **WHEN** a user uploads a file larger than the configured maximum
- **THEN** the response is HTTP 413

### Requirement: Extraction does not require a PREMIUM plan
PDF text extraction performs no AI call, so it SHALL be available to any authenticated user; the AI
plan gate applies only at generation.

#### Scenario: A FREE user can extract text
- **WHEN** a FREE-plan user uploads a text PDF
- **THEN** the text is extracted and returned with HTTP 200


## ADDED Requirements

### Requirement: Populate generation text from a PDF
The generate screen SHALL let the user upload a PDF; on success its extracted text fills the
generation text area, and the user proceeds with the existing generate → review → save flow.

#### Scenario: Uploading a PDF fills the text area
- **WHEN** the user uploads a text PDF on the generate screen
- **THEN** the extracted text appears in the generation text area, ready to generate

#### Scenario: Truncation is shown
- **WHEN** the uploaded PDF's text was truncated to the input limit
- **THEN** the screen indicates that the text was truncated

#### Scenario: A scanned or invalid PDF shows an error
- **WHEN** extraction fails (no extractable text, not a PDF, or too large)
- **THEN** the screen shows the error and the text area is left unchanged

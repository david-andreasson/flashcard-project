## 1. Dependency & config

- [x] 1.1 Add the Apache PDFBox dependency to `backend/pom.xml`.
- [x] 1.2 Configure multipart limits in `application.yml` (`spring.servlet.multipart.max-file-size` and `max-request-size`, e.g. 10 MB), with env overrides.
- [x] 1.3 Raise `ai.max-input-chars` to 24000 in `application.yml` (~6–8 A4 pages; quota still caps total spend).

## 2. Backend extraction

- [x] 2.1 DTO `ExtractPdfResponse(String text, int pageCount, int charCount, boolean truncated)`.
- [x] 2.2 `PdfExtractionService`: load the `MultipartFile` with PDFBox (`Loader.loadPDF` + `PDFTextStripper`); record page count; cap text to `ai.max-input-chars` (set `truncated`); throw `BadRequestException` on a blank result ("no extractable text") and on a parse failure.
- [x] 2.3 `PdfExtractionController`: `POST /ai/cards/extract-pdf` consuming multipart (`file` part); reject a non-PDF content type with 400; authenticated (no plan gate).
- [x] 2.4 Map `MaxUploadSizeExceededException` → HTTP 413 in `CommonExceptionHandler`.

## 3. Frontend

- [x] 3.1 Adjust `apiFetch` to NOT set `Content-Type: application/json` when the body is a `FormData` (keep the CSRF header).
- [x] 3.2 API client `extractPdf(file)` (multipart) returning `{ text, pageCount, charCount, truncated }`.
- [x] 3.3 AI Cards screen: an "Upload PDF" file picker that fills the generation text area on success, shows a truncation notice, and surfaces extraction errors without clearing the text area.

## 4. Tests

- [x] 4.1 `PdfExtractionService` test: build a small PDF in-test with PDFBox, extract → text + counts; long text → `truncated`; a no-text PDF → 400; a non-PDF / corrupt input → 400.
- [x] 4.2 Endpoint integration (MockMvc multipart): an authenticated user uploads a generated text PDF → 200 with text; a non-PDF part → 400.
- [x] 4.3 Gating: a FREE-plan user can extract (HTTP 200).
- [x] 4.4 Update the existing oversized-input tests to exceed the new 24000 limit: `AiPipelineIntegrationTest` (change 06) and `AiCardGenerationIntegrationTest` (change 07) currently use 9000 characters, which is now within bounds.

## 5. Config, docs, verification

- [x] 5.1 Document the multipart limits in `application.yml`.
- [x] 5.2 Update `docs/ARCHITECTURE.md` (PDF import flow, extract-and-discard, S3 deferred to 10) and mark change 09 on `docs/ROADMAP.md`.
- [x] 5.3 Run `./mvnw test` and the frontend build; confirm all green.

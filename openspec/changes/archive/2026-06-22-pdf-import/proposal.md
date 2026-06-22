## Why

Change 07 generates flashcards from pasted text. Pasting from a PDF (lecture notes, an article)
is tedious and loses structure. This change lets a user upload a PDF and have its text extracted
server-side, feeding the existing card-generation flow — so a PDF becomes just another text source.

## What Changes

- Add PDF text extraction with Apache PDFBox: `POST /api/ai/cards/extract-pdf` (multipart) returns
  the extracted text plus page count, character count, and a `truncated` flag.
- Extracted text is capped at the configured AI input limit (`ai.max-input-chars`), so what comes
  out is exactly what generation accepts; the response flags truncation so the user can edit first.
- Raise `ai.max-input-chars` from 8000 to 24000 (~6–8 A4 pages) so a multi-page PDF fits in one
  generation; the limit governs all AI input and the monthly token quota still caps total spend.
- PDFs with no embedded text (e.g. scanned images) and unreadable files are rejected with a clear
  error. OCR is out of scope.
- Multipart upload limits (file size, PDF content type) are enforced.
- The AI Cards screen gains an "Upload PDF" action that fills the existing generation text area;
  generation, review, and save are unchanged (reused from change 07).
- No persistence: the PDF is extracted and discarded. S3 storage is deferred to change 10.

## Capabilities

### New Capabilities
- `pdf-import`: server-side PDF → text extraction that feeds AI card generation.

### Modified Capabilities
- `ai-card-generation-frontend`: upload a PDF to populate the generation text.

## Impact

- Backend: Apache PDFBox dependency; a PDF-extraction controller + service; multipart limits in
  `application.yml`; an exception mapping for oversized uploads (413).
- Frontend: a PDF file picker on the AI Cards screen via the shared `apiFetch` helper (which must
  not force a JSON content type on multipart bodies).
- No database migration and no new storage. AI cost protection is unchanged — the guard wraps only
  generation, and extraction performs no AI call.

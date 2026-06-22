## Context

Change 07 delivers text → drafts → review → save (`POST /ai/cards/generate`, then bulk-save), with
the AI input capped at `ai.max-input-chars` (8000). This change adds a PDF as a new *source* of
that text; the generation, review, and save flow is reused unchanged. The backend has no multipart,
PDFBox, or storage infrastructure yet.

Decision taken with the user: **S3 storage is deferred to change 10.** Change 09 extracts text and
discards the PDF — no persistence.

## Goals / Non-Goals

**Goals:**
- Server-side PDF → text extraction that feeds the existing generation flow.
- Transparent truncation to the AI input limit; clear errors for bad/scanned/oversized files.
- A PDF upload entry point on the existing AI Cards screen.

**Non-Goals:**
- OCR for scanned/image PDFs.
- Persisting the PDF (S3) — change 10.
- One-shot PDF → drafts (we keep the two-step flow so the user reviews/edits the text first).
- Chunking a long PDF into multiple generations (multiplies AI cost) — truncation only, for now.

## Decisions

### 1. Two-step: extract endpoint returns text, generation is reused
`POST /api/ai/cards/extract-pdf` (multipart, field `file`) returns
`{ text, pageCount, charCount, truncated }`. The client drops `text` into the existing generation
text area; the user edits if needed and runs the unchanged generate → review → save flow.
- *Why:* the user sees and can trim the extracted text **before** spending AI tokens, and we reuse
  change 07 wholesale. *Alternative:* one-shot PDF → drafts — rejected (no pre-generation review, no
  control over what the 8000-char cap keeps).

### 2. Apache PDFBox, text PDFs only
Parse the `MultipartFile` stream with PDFBox (`Loader.loadPDF` + `PDFTextStripper`). Extracts
embedded text; scanned/image PDFs yield blank text and are rejected (see §4). No OCR.

### 3. Cap at the shared AI input limit
Truncate extracted text to `ai.max-input-chars` (the same limit generation enforces), so what the
endpoint returns is exactly what generation will accept. The response carries `truncated` plus the
original `charCount` so the UI can tell the user. *Alternative:* a separate PDF-specific limit —
rejected; reusing one limit avoids a confusing mismatch between "extracted" and "acceptable".

This change **raises `ai.max-input-chars` from 8000 to 24000** (~6–8 A4 pages of text, ~6000 input
tokens) so a multi-page PDF fits in a single generation. A full A4 page is only ~3,000–4,000
characters, so 8000 capped at ~2 pages. The limit still governs all AI input (paste, generation,
PDF), and the monthly token quota remains the spend ceiling.

### 4. Errors and validation
- **Not a PDF** (content type / extension) → 400.
- **Corrupt/unparseable** (PDFBox throws `IOException`) → 400 with a clear message.
- **No extractable text** (stripped text is blank) → 400, "no extractable text (likely a scanned
  PDF)". Reuses the existing `BadRequestException` (400); no new exception type.
- **Oversized** → Spring throws `MaxUploadSizeExceededException`; map it to **413** in the common
  exception handler (Spring's default would otherwise be a 500).
- Multipart limits set in `application.yml` (`spring.servlet.multipart.max-file-size` /
  `max-request-size`, e.g. 10 MB).

### 5. No persistence
The `MultipartFile` is read in-memory/stream and never written to disk or object storage. S3 is a
change-10 concern; if PDFs ever need to be retained (re-generation, audit) it is added there.

### 6. Gating
Extraction makes no AI call, so the endpoint requires only authentication (any plan). The PREMIUM /
ADMIN gate stays where the cost is — at generation. A FREE user can extract text but will be gated
at the generate step (existing 403).

### 7. Frontend multipart through `apiFetch`
The shared `apiFetch` sets `Content-Type: application/json` when a body is present and none is set —
which would corrupt a multipart upload (the browser must set the `multipart/form-data` boundary).
`apiFetch` is adjusted to skip that default when the body is a `FormData`. The CSRF `X-XSRF-TOKEN`
header is still added (extraction is a state-changing POST).

## Risks / Trade-offs

- **Messy extraction** (multi-column layouts, ligatures, headers/footers) → the user edits the text
  in the area before generating; we don't try to clean it server-side.
- **Memory on large PDFs** → bounded by the multipart `max-file-size`; PDFBox loads the document
  within that bound.
- **Oversized-upload status** → must verify `MaxUploadSizeExceededException` maps to 413 (easy to
  miss; Spring's default is 500).
- **FormData + apiFetch** → if the JSON content-type isn't suppressed for `FormData`, the upload
  fails; covered by decision §7 and a test of the helper's behavior.
- **Raising the cap breaks existing oversized-input tests** → change 06 `AiPipelineIntegrationTest`
  and change 07 `AiCardGenerationIntegrationTest` use 9000 characters as "oversized" (was above the
  old 8000 limit). With the limit at 24000, 9000 is now valid, so those tests must be updated to
  exceed 24000.

## Migration Plan

- No database migration. New dependency (Apache PDFBox) and multipart config only.
- No new storage; nothing to roll back beyond removing the endpoint, dependency, and config.

## Open Questions

- `max-file-size` value — proposed 10 MB; adjust if needed.
- Replace vs append when filling the text area — proposed replace (simplest); revisit if users want
  to combine a paste and a PDF.

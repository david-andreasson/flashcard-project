package com.flashcard.pdf.dto;

/**
 * Result of extracting text from an uploaded PDF. {@code charCount} is the full extracted length
 * (before any cap); {@code text} is capped to the AI input limit and {@code truncated} says whether
 * the cap removed anything.
 */
public record ExtractPdfResponse(String text, int pageCount, int charCount, boolean truncated) {
}

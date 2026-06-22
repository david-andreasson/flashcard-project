package com.flashcard.pdf;

import com.flashcard.ai.AiProperties;
import com.flashcard.common.BadRequestException;
import com.flashcard.pdf.dto.ExtractPdfResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extracts embedded text from a PDF and caps it to the AI input limit so it can feed card
 * generation. Reads the upload in memory and discards it — nothing is persisted. Scanned/image PDFs
 * (no text layer) and unreadable files surface as a 400; OCR is not provided.
 */
@Service
public class PdfExtractionService {

    private final AiProperties aiProperties;

    public PdfExtractionService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    public ExtractPdfResponse extract(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        String rawText;
        int pageCount;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            pageCount = document.getNumberOfPages();
            rawText = new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new BadRequestException("Could not parse the file as a PDF");
        }

        String text = rawText == null ? "" : rawText.strip();
        if (text.isEmpty()) {
            throw new BadRequestException("No extractable text found — the PDF may be a scanned image");
        }

        int limit = aiProperties.maxInputChars();
        boolean truncated = text.length() > limit;
        String capped = truncated ? text.substring(0, limit) : text;
        return new ExtractPdfResponse(capped, pageCount, text.length(), truncated);
    }
}

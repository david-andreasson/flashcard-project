package com.flashcard.pdf;

import com.flashcard.common.BadRequestException;
import com.flashcard.pdf.dto.ExtractPdfResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extracts text from an uploaded PDF for use in card generation. Any authenticated user may call
 * it — there is no AI call here, so the PREMIUM/ADMIN gate applies only at generation. Oversized
 * uploads are rejected by the multipart limits (mapped to 413 in {@code CommonExceptionHandler}).
 */
@RestController
@RequestMapping("/ai/cards")
public class PdfExtractionController {

    private final PdfExtractionService service;

    public PdfExtractionController(PdfExtractionService service) {
        this.service = service;
    }

    @PostMapping(value = "/extract-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractPdfResponse extract(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("No file uploaded");
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        boolean isPdf = MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)
                || (name != null && name.toLowerCase().endsWith(".pdf"));
        if (!isPdf) {
            throw new BadRequestException("Only PDF files are supported");
        }
        return service.extract(file);
    }
}

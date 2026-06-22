package com.flashcard.pdf;

import com.flashcard.ai.AiProperties;
import com.flashcard.common.BadRequestException;
import com.flashcard.pdf.dto.ExtractPdfResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfExtractionServiceTest {

    private static AiProperties props(int maxInputChars) {
        return new AiProperties(true, "mock", maxInputChars, 1024,
                new AiProperties.Pricing(BigDecimal.ZERO, BigDecimal.ZERO),
                new AiProperties.Quota(0L, 200000L, 100000000L));
    }

    static byte[] pdfWithText(String text) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] blankPdf() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static MockMultipartFile file(byte[] bytes) {
        return new MockMultipartFile("file", "test.pdf", "application/pdf", bytes);
    }

    @Test
    void extractsText_withCounts_notTruncated() throws Exception {
        PdfExtractionService svc = new PdfExtractionService(props(24000));
        ExtractPdfResponse res = svc.extract(file(pdfWithText("Hello PDF world")));
        assertThat(res.text()).contains("Hello PDF world");
        assertThat(res.pageCount()).isEqualTo(1);
        assertThat(res.truncated()).isFalse();
        assertThat(res.charCount()).isEqualTo(res.text().length());
    }

    @Test
    void truncatesToTheLimit() throws Exception {
        PdfExtractionService svc = new PdfExtractionService(props(5));
        ExtractPdfResponse res = svc.extract(file(pdfWithText("Hello PDF world")));
        assertThat(res.truncated()).isTrue();
        assertThat(res.text()).hasSize(5);
        assertThat(res.charCount()).isGreaterThan(5);
    }

    @Test
    void blankPdf_rejected() throws Exception {
        PdfExtractionService svc = new PdfExtractionService(props(24000));
        assertThatThrownBy(() -> svc.extract(file(blankPdf())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void nonPdfBytes_rejected() {
        PdfExtractionService svc = new PdfExtractionService(props(24000));
        assertThatThrownBy(() -> svc.extract(file("this is not a pdf".getBytes())))
                .isInstanceOf(BadRequestException.class);
    }
}

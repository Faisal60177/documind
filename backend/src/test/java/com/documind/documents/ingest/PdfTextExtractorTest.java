package com.documind.documents.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extractsTextAndPageCount() throws IOException {
        byte[] pdf = pdfWithPages("Hello DocuMind", "Second page text");

        ExtractedPdf result = extractor.extract(pdf, 10);

        assertThat(result.pageCount()).isEqualTo(2);
        assertThat(result.pages()).hasSize(2);
        assertThat(result.pages().get(0).page()).isEqualTo(1);
        assertThat(result.pages().get(0).text()).contains("Hello DocuMind");
        assertThat(result.pages().get(1).text()).contains("Second page text");
    }

    @Test
    void rejectsPdfsOverThePageLimit() throws IOException {
        byte[] pdf = pdfWithPages("one", "two", "three");

        assertThatThrownBy(() -> extractor.extract(pdf, 2))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("3 pages");
    }

    @Test
    void rejectsFilesThatAreNotPdfs() {
        assertThatThrownBy(() -> extractor.extract("not a pdf".getBytes(), 10))
                .isInstanceOf(IngestionException.class);
    }

    private static byte[] pdfWithPages(String... texts) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String text : texts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}

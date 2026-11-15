package com.documind.documents.ingest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    private static final Pattern SINGLE_NEWLINE = Pattern.compile("(?<!\\n)\\n(?!\\n)");
    private static final Pattern SPACES = Pattern.compile("[ \\t\\u00A0]+");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");

    public ExtractedPdf extract(byte[] pdf, int maxPages) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int pageCount = document.getNumberOfPages();
            if (pageCount > maxPages) {
                throw new IngestionException(
                        "This PDF has " + pageCount + " pages. The limit is " + maxPages + ".");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = clean(stripper.getText(document));
                if (!text.isEmpty()) {
                    pages.add(new PageText(page, text));
                }
            }
            return new ExtractedPdf(pageCount, pages);
        } catch (InvalidPasswordException ex) {
            throw new IngestionException("This PDF is password protected.", ex);
        } catch (IOException ex) {
            throw new IngestionException("This file could not be read as a PDF.", ex);
        }
    }

    /** Normalizes line breaks and whitespace, and removes characters PostgreSQL cannot store. */
    static String clean(String raw) {
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        text = CONTROL_CHARACTERS.matcher(text).replaceAll("");
        text = SINGLE_NEWLINE.matcher(text).replaceAll(" ");
        text = SPACES.matcher(text).replaceAll(" ");
        text = BLANK_LINES.matcher(text).replaceAll("\n\n");
        return text.strip();
    }
}

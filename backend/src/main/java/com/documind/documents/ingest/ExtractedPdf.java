package com.documind.documents.ingest;

import java.util.List;

/** Text extracted from a PDF. Pages without any text are left out of {@code pages}. */
public record ExtractedPdf(int pageCount, List<PageText> pages) {}

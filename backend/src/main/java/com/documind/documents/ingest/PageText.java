package com.documind.documents.ingest;

/** The cleaned text of one PDF page (pages are numbered from 1). */
public record PageText(int page, String text) {}

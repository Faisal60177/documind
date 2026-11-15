package com.documind.documents.ingest;

/** A passage of a document. It may start and end on different pages. */
public record TextChunk(int index, int pageStart, int pageEnd, String content, int tokens) {}

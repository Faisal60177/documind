package com.documind.rag;

public enum AnswerStatus {
    /** The documents contain an answer; sources are attached. */
    ANSWERED,
    /** The user has searchable documents, but none contain the answer. */
    NOT_FOUND,
    /** The user has no searchable documents yet. */
    NO_DOCUMENTS
}

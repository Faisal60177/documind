package com.documind.rag;

import java.util.List;

public record AskResponse(AnswerStatus status, String answer, List<SourceResponse> sources) {}

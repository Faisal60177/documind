package com.documind.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank(message = "Type a question first")
        @Size(max = 1000, message = "The question must be at most 1000 characters")
        String question) {}

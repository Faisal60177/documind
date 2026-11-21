package com.documind.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        @Positive @Max(20) int topK,
        @NotBlank String generationModel,
        String thinkingLevel,
        @Positive int maxOutputTokens,
        @Positive int snippetLength,
        @NotNull Duration generationTimeout,
        @PositiveOrZero int generationMaxRetries,
        @NotNull Duration generationBackoff) {}

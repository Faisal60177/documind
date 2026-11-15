package com.documind.documents.ingest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        String apiKey,
        @NotBlank String baseUrl,
        @NotBlank String embeddingModel,
        @Positive int embeddingDimensions,
        @Positive int batchSize,
        @NotNull Duration batchDelay,
        @PositiveOrZero int maxRetries,
        @NotNull Duration initialBackoff,
        @NotNull Duration requestTimeout) {}

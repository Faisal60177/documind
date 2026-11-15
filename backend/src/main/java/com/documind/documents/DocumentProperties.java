package com.documind.documents;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.documents")
public record DocumentProperties(
        @NotNull DataSize maxSize,
        @Positive int maxPages,
        @Positive int chunkMaxTokens,
        @PositiveOrZero int chunkOverlapTokens) {}

package com.zufar.icedlatte.review.config;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.usage")
public record AiUsageProperties(
        @Min(0) @DefaultValue("100") long dailyCalls,
        @Min(1) @DefaultValue("2") int concurrentCalls,
        @Min(1) @DefaultValue("20000") int maxInputChars,
        @Min(1) @DefaultValue("256") int maxOutputTokens) {}

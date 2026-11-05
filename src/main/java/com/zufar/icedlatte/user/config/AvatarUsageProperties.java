package com.zufar.icedlatte.user.config;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "avatar.usage")
public record AvatarUsageProperties(
        @Min(0) @DefaultValue("100") long dailyUploads,
        @Min(0) @DefaultValue("10") long dailyUploadsPerUser,
        @Min(1) @DefaultValue("3") int liveIntentsPerUser) {}

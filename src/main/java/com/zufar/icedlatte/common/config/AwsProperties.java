package com.zufar.icedlatte.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.aws")
public record AwsProperties(

        @NotBlank(message = "AWS access-key must not be blank")
        String accessKey,

        @NotBlank(message = "AWS secret-key must not be blank")
        String secretKey,

        @NotBlank(message = "AWS region must not be blank")
        String region,

        String endpointUrl
) {}

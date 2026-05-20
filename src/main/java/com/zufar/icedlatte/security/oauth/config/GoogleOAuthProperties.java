package com.zufar.icedlatte.security.oauth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "google")
public record GoogleOAuthProperties(

        @NotBlank(message = "Google client-id must not be blank")
        String clientId,

        @NotBlank(message = "Google client-secret must not be blank")
        String clientSecret,

        @NotBlank(message = "Google redirect-uri must not be blank")
        String redirectUri,

        @NotBlank(message = "Google scope must not be blank")
        String scope,

        Auth auth
) {

    public record Auth(Server server) {
        public record Server(@NotBlank(message = "Google auth server url must not be blank") String url) {
        }
    }
}

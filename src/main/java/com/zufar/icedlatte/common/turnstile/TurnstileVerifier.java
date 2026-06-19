package com.zufar.icedlatte.common.turnstile;

import java.time.Duration;

import jakarta.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifies Cloudflare Turnstile tokens via the siteverify API. Disabled (no-op) when {@code turnstile.enabled=false}.
 */
@Slf4j
@Component
public class TurnstileVerifier {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final boolean enabled;
    private final String secretKey;
    private final RestClient restClient;

    @Autowired
    public TurnstileVerifier(TurnstileProperties properties) {
        this(properties, restClient(properties.connectTimeout(), properties.readTimeout()));
    }

    TurnstileVerifier(TurnstileProperties properties, RestClient restClient) {
        this.enabled = properties.enabled();
        this.secretKey = properties.secretKey();
        this.restClient = restClient;
    }

    private static RestClient restClient(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public void verify(@Nullable String token) {
        verify(token, null);
    }

    public void verify(@Nullable String token, @Nullable String remoteIp) {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new TurnstileVerificationException("Turnstile verification required");
        }
        try {
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secretKey);
            form.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                form.add("remoteip", remoteIp);
            }

            TurnstileResponse result = restClient
                    .post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);
            if (result == null || !result.success()) {
                log.info("turnstile.verification.failed");
                throw new TurnstileVerificationException("Turnstile verification failed");
            }
        } catch (TurnstileVerificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("turnstile.service.error: {}", e.getMessage());
            throw new TurnstileVerificationException("Turnstile service unavailable");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TurnstileResponse(
            @JsonProperty("success") boolean success) {}
}

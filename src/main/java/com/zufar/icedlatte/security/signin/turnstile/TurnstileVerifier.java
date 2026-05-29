package com.zufar.icedlatte.security.signin.turnstile;

import java.time.Duration;

import jakarta.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zufar.icedlatte.security.signin.exception.TurnstileVerificationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifies Cloudflare Turnstile tokens via the siteverify API. Disabled (no-op) when {@code turnstile.secret-key} is
 * blank.
 */
@Slf4j
@Component
public class TurnstileVerifier {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final boolean enabled;
    private final String secretKey;
    private final RestClient restClient;

    @Autowired
    public TurnstileVerifier(
            @Value("${turnstile.enabled:false}") boolean enabled,
            @Value("${turnstile.secret-key:}") String secretKey,
            @Value("${turnstile.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${turnstile.read-timeout:PT3S}") Duration readTimeout) {
        this(enabled, secretKey, restClient(connectTimeout, readTimeout));
    }

    TurnstileVerifier(String secretKey) {
        this(!secretKey.isBlank(), secretKey, restClient(Duration.ofSeconds(2), Duration.ofSeconds(3)));
    }

    TurnstileVerifier(boolean enabled, String secretKey, RestClient restClient) {
        if (enabled && secretKey.isBlank()) {
            throw new IllegalStateException("turnstile.secret-key must be configured when turnstile.enabled=true");
        }
        this.enabled = enabled;
        this.secretKey = secretKey;
        this.restClient = restClient;
    }

    private static RestClient restClient(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public void verify(@Nullable String token) {
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

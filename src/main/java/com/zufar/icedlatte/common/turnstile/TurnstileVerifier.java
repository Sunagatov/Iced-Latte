package com.zufar.icedlatte.common.turnstile;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Verifies Cloudflare Turnstile tokens via the siteverify API. Disabled (no-op) when {@code turnstile.enabled=false}.
 */
@Slf4j
@Component
public class TurnstileVerifier {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final int MAX_TOKEN_LENGTH = 2048;

    private final boolean enabled;
    private final boolean validateActions;
    private final String secretKey;
    private final List<String> expectedHostnames;
    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    public TurnstileVerifier(TurnstileProperties properties) {
        this(properties, restClient(properties.connectTimeout(), properties.readTimeout()), new SimpleMeterRegistry());
    }

    @Autowired
    public TurnstileVerifier(TurnstileProperties properties, MeterRegistry meterRegistry) {
        this(properties, restClient(properties.connectTimeout(), properties.readTimeout()), meterRegistry);
    }

    TurnstileVerifier(TurnstileProperties properties, RestClient restClient) {
        this(properties, restClient, new SimpleMeterRegistry());
    }

    TurnstileVerifier(TurnstileProperties properties, RestClient restClient, MeterRegistry meterRegistry) {
        this.enabled = properties.enabled();
        this.validateActions = properties.validateActions();
        this.secretKey = properties.secretKey();
        this.expectedHostnames = properties.expectedHostnames();
        this.restClient = restClient;
        this.meterRegistry = meterRegistry;
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
        verify(TurnstileVerificationRequest.basic(token, remoteIp));
    }

    public void verify(TurnstileVerificationRequest request) {
        if (!enabled) {
            return;
        }
        if (request.token() == null || request.token().isBlank()) {
            recordVerification(request.source(), "missing_token");
            throw new TurnstileVerificationException("Turnstile verification required");
        }
        if (request.token().length() > MAX_TOKEN_LENGTH) {
            recordVerification(request.source(), "invalid_token");
            throw new TurnstileVerificationException("Turnstile verification failed");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secretKey);
            form.add("response", request.token());
            form.add("idempotency_key", UUID.randomUUID().toString());
            if (request.remoteIp() != null) {
                form.add("remoteip", request.remoteIp());
            }

            TurnstileResponse result = restClient
                    .post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);
            if (result == null) {
                recordVerification(request.source(), "empty_response");
                log.info("turnstile.verification.failed: source={}, reason=empty_response", request.source());
                throw new TurnstileVerificationException("Turnstile verification failed");
            }
            if (!result.success()) {
                recordVerification(request.source(), "rejected");
                log.info(
                        "turnstile.verification.failed: source={}, reason=rejected, errorCodes={}",
                        request.source(),
                        result.errorCodes());
                throw new TurnstileVerificationException("Turnstile verification failed");
            }
            if (!matchesExpectedHostname(result.hostname())) {
                recordVerification(request.source(), "hostname_mismatch");
                log.warn(
                        "turnstile.verification.failed: source={}, reason=hostname_mismatch, hostname={}, expectedHostnames={}",
                        request.source(),
                        result.hostname(),
                        expectedHostnames);
                throw new TurnstileVerificationException("Turnstile verification failed");
            }
            if (validateActions
                    && request.expectedAction() != null
                    && !request.expectedAction().equals(result.action())) {
                recordVerification(request.source(), "action_mismatch");
                log.warn(
                        "turnstile.verification.failed: source={}, reason=action_mismatch, expectedAction={}, actualAction={}",
                        request.source(),
                        request.expectedAction(),
                        result.action());
                throw new TurnstileVerificationException("Turnstile verification failed");
            }
            recordVerification(request.source(), "success");
        } catch (TurnstileVerificationException e) {
            throw e;
        } catch (Exception e) {
            recordVerification(request.source(), "unavailable");
            log.error("turnstile.service.error: source={}, message={}", request.source(), e.getMessage());
            throw new TurnstileVerificationException("Turnstile service unavailable");
        } finally {
            sample.stop(meterRegistry.timer("security.turnstile.siteverify.latency", "source", request.source()));
        }
    }

    private boolean matchesExpectedHostname(@Nullable String hostname) {
        if (expectedHostnames.isEmpty()) {
            return true;
        }
        if (hostname == null || hostname.isBlank()) {
            return false;
        }
        return expectedHostnames.contains(hostname.trim().toLowerCase(Locale.ROOT));
    }

    private void recordVerification(String source, String outcome) {
        meterRegistry
                .counter("security.turnstile.verifications", "source", source, "outcome", outcome)
                .increment();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TurnstileResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("hostname") @Nullable String hostname,
            @JsonProperty("action") @Nullable String action,
            @JsonProperty("error-codes") List<String> errorCodes) {

        private TurnstileResponse {
            errorCodes = errorCodes == null ? List.of() : List.copyOf(errorCodes);
        }
    }
}

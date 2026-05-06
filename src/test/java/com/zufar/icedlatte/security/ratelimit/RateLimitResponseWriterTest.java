package com.zufar.icedlatte.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitResponseWriter unit tests")
class RateLimitResponseWriterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("writes normalized rate-limit headers")
    void writesRateLimitHeaders() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        long resetTimeMillis = System.currentTimeMillis() + 30_000;
        RateLimitResult result = new RateLimitResult(true, 60, -5, resetTimeMillis);

        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("60");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        // delay-seconds: ~30s remaining
        long resetSeconds = Long.parseLong(Objects.requireNonNull(response.getHeader("X-RateLimit-Reset")));
        assertThat(resetSeconds).isBetween(28L, 31L);
        // IETF structured fields
        assertThat(response.getHeader("RateLimit-Policy")).startsWith("\"default\";q=60;w=");
        assertThat(response.getHeader("RateLimit")).startsWith("\"default\";r=0;t=");
    }

    @Test
    @DisplayName("writes a complete 429 JSON response with retry metadata")
    void writesTooManyRequestsResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        long resetTimeMillis = System.currentTimeMillis() + 3_000;
        RateLimitResult result = new RateLimitResult(false, 10, 0, resetTimeMillis);

        RateLimitResponseWriter.writeTooManyRequests(response, result, "https://errors.example.test/problems/rate-limited");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("Retry-After")).isNotBlank();

        var json = OBJECT_MAPPER.readTree(response.getContentAsByteArray());
        assertThat(json.get("type").asText()).isEqualTo("https://errors.example.test/problems/rate-limited");
        assertThat(json.get("title").asText()).isEqualTo("Too many requests");
        assertThat(json.get("detail").asText()).isEqualTo("Too many requests. Please try again later.");
        assertThat(json.get("status").asInt()).isEqualTo(429);
        assertThat(json.get("retryAfter").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(json.get("timestamp").asText()).isNotBlank();
        assertThat(response.getContentLength()).isEqualTo(response.getContentAsByteArray().length);
    }
}

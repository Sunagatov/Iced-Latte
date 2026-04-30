package com.zufar.icedlatte.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zufar.icedlatte.security.ratelimit.RateLimitingConfiguration.RateLimitResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Shared 429 response writer used by both rate-limiting filters.
 * Keeps the response format consistent across pre-auth and post-auth paths.
 */
@UtilityClass
public class RateLimitResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void writeRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining())));
        response.setHeader("X-RateLimit-Reset", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis())));
    }

    public static void writeTooManyRequests(HttpServletResponse response,
                                            RateLimitResult result) throws IOException {
        long retryAfterSeconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));
        writeRateLimitHeaders(response, result);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        ObjectNode json = OBJECT_MAPPER.createObjectNode()
                .put("error", "Rate limit exceeded")
                .put("message", "Too many requests. Please try again later.")
                .put("status", HttpStatus.TOO_MANY_REQUESTS.value())
                .put("timestamp", Instant.now().toString())
                .put("retryAfter", retryAfterSeconds);
        byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(json);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}

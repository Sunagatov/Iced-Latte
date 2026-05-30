package com.zufar.icedlatte.security.config;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;

@Component
@RequiredArgsConstructor
public class SecurityProblemResponseWriter {

    private final ObjectMapper objectMapper;
    private final ProblemTypeUriFactory problemTypeUriFactory;

    public void write(
            HttpServletResponse response,
            int status,
            String typeSlug,
            String title,
            String detail,
            String path,
            String requestId)
            throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        ObjectNode json = objectMapper
                .createObjectNode()
                .put("type", problemTypeUriFactory.build(typeSlug))
                .put("title", title)
                .put("status", status)
                .put("detail", detail)
                .put("instance", path)
                .put("timestamp", Instant.now().toString());

        if (requestId != null) {
            json.put("requestId", requestId);
        }

        byte[] bytes = objectMapper.writeValueAsBytes(json);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}

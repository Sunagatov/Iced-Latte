package com.zufar.icedlatte.review.config;

import static org.assertj.core.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

class AiEnabledConfigTest {
    @Test
    void capsOutputAndDisablesHiddenProviderRetries() throws Exception {
        var calls = new AtomicInteger();
        var request = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"error\":{\"message\":\"test failure\",\"type\":\"server_error\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var model = new AiEnabledConfig()
                    .openAiChatModel(
                            "local-test-only",
                            "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                            "test-model",
                            new AiUsageProperties(100, 2, 20000, 256));
            assertThatThrownBy(() -> model.chat("hello")).isInstanceOf(RuntimeException.class);
            assertThat(calls.get()).isEqualTo(1);
            assertThat(new ObjectMapper()
                            .readTree(request.get())
                            .get("max_tokens")
                            .asInt())
                    .isEqualTo(256);
        } finally {
            server.stop(0);
        }
    }
}

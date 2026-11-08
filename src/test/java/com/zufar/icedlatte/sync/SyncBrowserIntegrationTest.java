package com.zufar.icedlatte.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

/** Actual browser -> Next proxy -> Spring -> disposable PostgreSQL. External mail/storage stay mocked. */
@EnabledIfSystemProperty(named = "sync.browser", matches = "true")
class SyncBrowserIntegrationTest extends AuthenticatedUserIntegrationSupport {
    @Autowired
    ObjectMapper mapper;

    @Test
    void realFrontendAndBackendRecoverLostResponsesAndConflicts() throws Exception {
        // Private image bytes remain local, while uploads and profile revisions
        // still traverse the real proxy, application and database.
        var avatarObjects = new java.util.concurrent.ConcurrentHashMap<String, byte[]>();
        var imageServer = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        imageServer.createContext("/avatar/", exchange -> {
            byte[] bytes = avatarObjects.get(exchange.getRequestURI().getPath().substring("/avatar/".length()));
            if (bytes == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        org.mockito.Mockito.when(objectStorage.isConfigured()).thenReturn(true);
        org.mockito.Mockito.doAnswer(call -> {
                    org.springframework.web.multipart.MultipartFile file = call.getArgument(0);
                    avatarObjects.put(call.getArgument(2), file.getBytes());
                    return null;
                })
                .when(objectStorage)
                .upload(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.when(objectStorage.getUrl(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> {
                    com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto metadata = call.getArgument(0);
                    return avatarObjects.containsKey(metadata.fileName())
                            ? java.util.Optional.of("http://127.0.0.1:"
                                    + imageServer.getAddress().getPort() + "/avatar/" + metadata.fileName())
                            : java.util.Optional.empty();
                });
        Path frontend = Path.of(System.getProperty(
                        "sync.frontend",
                        System.getenv().getOrDefault("ICED_LATTE_FRONTEND_SOURCE", "../Iced-Latte-Frontend")))
                .toAbsolutePath()
                .normalize();
        assertThat(frontend.resolve("node_modules/next/dist/bin/next")).isRegularFile();
        Path generatedTypes = frontend.resolve("next-env.d.ts");
        byte[] originalTypes = Files.readAllBytes(generatedTypes);
        Path output = Path.of("target/sync-browser").toAbsolutePath();
        Files.createDirectories(output);
        int frontendPort;
        try (var socket = new ServerSocket(0)) {
            frontendPort = socket.getLocalPort();
        }
        String url = "http://127.0.0.1:" + frontendPort;
        String node = System.getProperty("sync.node", "node");
        var builder = new ProcessBuilder(
                node,
                "node_modules/next/dist/bin/next",
                "dev",
                "--hostname",
                "127.0.0.1",
                "--port",
                Integer.toString(frontendPort));
        builder.directory(frontend.toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.resolve("next.log").toFile());
        builder.environment()
                .putAll(Map.of(
                        "INTERNAL_API_URL",
                        "http://127.0.0.1:" + port + "/api/v1",
                        "NEXT_PUBLIC_API_URL",
                        "http://127.0.0.1:" + port + "/api/v1",
                        "NEXT_PUBLIC_FRONTEND_URL",
                        url,
                        "NEXT_PUBLIC_SENTRY_DSN",
                        "",
                        "SENTRY_DSN",
                        "",
                        "NEXT_PUBLIC_STRIPE_ENABLED",
                        "false",
                        "NEXT_PUBLIC_GOOGLE_AUTH_ENABLED",
                        "false"));
        Process server = builder.start();
        Process browser = null;
        imageServer.start();
        try {
            try (var http =
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
                long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
                boolean ready = false;
                while (System.nanoTime() < deadline && server.isAlive()) {
                    try {
                        if (http.send(
                                                HttpRequest.newBuilder(URI.create(url + "/signin"))
                                                        .timeout(Duration.ofSeconds(5))
                                                        .build(),
                                                HttpResponse.BodyHandlers.discarding())
                                        .statusCode()
                                == 200) {
                            ready = true;
                            break;
                        }
                    } catch (Exception ignored) {
                        Thread.sleep(250);
                    }
                }
                assertThat(ready)
                        .as("Next local server ready; see target/sync-browser/next.log")
                        .isTrue();
            }
            var accounts = new ArrayList<Map<String, String>>();
            for (int i = 0; i < 8; i++) {
                var user = registerAndAuthenticateUser();
                accounts.add(Map.of("token", user.accessToken(), "refreshToken", user.refreshToken()));
            }
            browser = new ProcessBuilder(node, "e2e/sync-lifecycle.real.mjs")
                    .directory(frontend.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(output.resolve("browser.log").toFile())
                    .start();
            // Disposable test credentials travel over stdin; never print them or put them in argv.
            try (var stdin = browser.outputWriter(StandardCharsets.UTF_8)) {
                stdin.write(mapper.writeValueAsString(
                        Map.of("url", url, "output", output.toString(), "accounts", accounts)));
            }
            assertThat(browser.waitFor(180, TimeUnit.SECONDS))
                    .as("browser completed; see target/sync-browser/browser.log")
                    .isTrue();
            assertThat(browser.exitValue())
                    .as("real browser checks; see target/sync-browser/browser.log")
                    .isZero();
        } finally {
            imageServer.stop(0);
            if (browser != null) stop(browser);
            stop(server);
            String before = new String(originalTypes, StandardCharsets.UTF_8);
            String after = Files.readString(generatedTypes);
            // Next dev changes only its generated types import. Restore that
            // exact known change, never concurrent edits to any source file.
            if (!before.equals(after)) {
                assertThat(after.replace(".next/dev/types/", ".next/types/").replace("\r\n", "\n"))
                        .as("Only Next's generated type import changed")
                        .isEqualTo(before.replace(".next/dev/types/", ".next/types/")
                                .replace("\r\n", "\n"));
                Files.write(generatedTypes, originalTypes);
            }
        }
    }

    private static void stop(Process process) {
        process.descendants().forEach(handle -> handle.destroyForcibly());
        process.destroyForcibly();
    }
}

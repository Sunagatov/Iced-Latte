package com.zufar.icedlatte.security.endpoint;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

class MailQueueSaturationIntegrationTest extends AuthenticatedUserIntegrationSupport {
    @Autowired
    @Qualifier("authEmailExecutor")
    ThreadPoolTaskExecutor executor;

    @Test
    void saturationDoesNotDiscloseMembershipOrFailCommittedPasswordChange() throws Exception {
        var user = registerAndAuthenticateUser();
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocked = () -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            for (int i = 0; i < 66; i++) executor.execute(blocked);
            assertThatThrownBy(() -> executor.execute(blocked)).isInstanceOf(TaskRejectedException.class);
            given(jsonSpec("/api/v1/auth/password"))
                    .body(Map.of("email", user.email()))
                    .post("/forgot")
                    .then()
                    .statusCode(200);
            given(jsonSpec("/api/v1/auth/password"))
                    .body(Map.of("email", "missing-queue@example.com"))
                    .post("/forgot")
                    .then()
                    .statusCode(200);
            given(authenticatedJsonSpec("/api/v1", user.accessToken()))
                    .body(Map.of("oldPassword", user.password(), "newPassword", "gentle river coffee morning"))
                    .patch("/users")
                    .then()
                    .statusCode(200);
            given(authenticatedJsonSpec("/api/v1", user.accessToken()))
                    .get("/users")
                    .then()
                    .statusCode(401);
            given(jsonSpec("/api/v1/auth"))
                    .body(Map.of("email", user.email(), "password", "gentle river coffee morning"))
                    .post("/authenticate")
                    .then()
                    .statusCode(200);
        } finally {
            release.countDown();
        }
    }
}

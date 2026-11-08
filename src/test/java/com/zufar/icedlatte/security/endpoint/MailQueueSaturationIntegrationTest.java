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

@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class MailQueueSaturationIntegrationTest extends AuthenticatedUserIntegrationSupport {
    @Autowired
    @Qualifier("authEmailExecutor")
    ThreadPoolTaskExecutor executor;

    @Test
    void saturationDoesNotDiscloseMembershipOrFailCommittedPasswordChange() throws Exception {
        var user = registerAndAuthenticateUser();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(2);
        Runnable blocked = () -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            executor.execute(blocked);
            executor.execute(blocked);
            org.assertj.core.api.Assertions.assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS))
                    .isTrue();
            boolean saturated = false;
            for (int i = 0; i < 128; i++) {
                try {
                    executor.execute(blocked);
                } catch (TaskRejectedException expected) {
                    saturated = true;
                    break;
                }
            }
            org.assertj.core.api.Assertions.assertThat(saturated).isTrue();
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

package com.zufar.icedlatte.user.endpoint;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;

import io.restassured.http.ContentType;

@DisplayName("User avatar endpoint integration tests")
class UserAvatarEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String BASE_PATH = "/api/v1/users";

    @Autowired
    private UserLookupApi userLookupApi;

    @Autowired
    private UserAvatarUploadRepository userAvatarUploadRepository;

    @DynamicPropertySource
    static void avatarProperties(DynamicPropertyRegistry registry) {
        registry.add("avatar.upload-mode", () -> "backend");
    }

    @Test
    @DisplayName("Should return not found when avatar does not exist")
    void shouldReturnNotFoundWhenAvatarDoesNotExist() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .get("/avatar")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should delete avatar successfully when avatar does not exist")
    void shouldDeleteAvatarSuccessfullyWhenAvatarDoesNotExist() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .delete("/avatar")
                .then()
                .statusCode(HttpStatus.OK.value());

        given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .get("/avatar")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should return bad request for invalid avatar content type")
    void shouldReturnBadRequestForInvalidAvatarContentType() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .multiPart("file", "avatar.txt", "not-an-image".getBytes(), "text/plain")
                .post("/avatar")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should return bad request for invalid avatar magic bytes")
    void shouldReturnBadRequestForInvalidAvatarMagicBytes() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .multiPart("file", "avatar.jpg", "fake-jpeg-content".getBytes(), "image/jpeg")
                .post("/avatar")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail closed for avatar upload intent in backend mode")
    void shouldFailClosedForAvatarUploadIntentInBackendMode() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .header("Idempotency-Key", "avatar-key-1")
                .body("""
                        {
                          "contentType": "image/png",
                          "sizeBytes": 1024
                        }
                        """)
                .post("/avatar/uploads")
                .then()
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .body("title", equalTo("File upload failed"))
                .body("detail", equalTo("An internal server error occurred."));

        assertThat(uploadsForUser(userId)).isEmpty();
    }

    @Test
    @DisplayName("Should return bad request when avatar upload intent header is missing")
    void shouldReturnBadRequestWhenAvatarUploadIntentHeaderIsMissing() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body("""
                        {
                          "contentType": "image/png",
                          "sizeBytes": 1024
                        }
                        """)
                .post("/avatar/uploads")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", notNullValue());
    }

    @Test
    @DisplayName("Should return not found when avatar upload status does not exist")
    void shouldReturnNotFoundWhenAvatarUploadStatusDoesNotExist() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .accept(ContentType.JSON)
                .get("/avatar/uploads/{uploadId}", UUID.randomUUID())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should cancel pending avatar upload for the owning user")
    void shouldCancelPendingAvatarUploadForOwningUser() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        UserAvatarUpload upload = userAvatarUploadRepository.save(upload(
                userId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.PENDING_UPLOAD,
                Instant.parse("2026-06-28T11:00:00Z"),
                Instant.parse("2026-06-28T11:05:00Z")));

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .delete("/avatar/uploads/{uploadId}", upload.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(userAvatarUploadRepository.findById(upload.getId()))
                .get()
                .matches(saved -> saved.getStatus() == UserAvatarUploadStatus.SUPERSEDED)
                .matches(saved -> saved.getSupersededAt() != null);
    }

    @Test
    @DisplayName("Should not reveal whether another user owns avatar upload cancellation target")
    void shouldHideAvatarUploadCancellationTargetFromAnotherUser() {
        AuthenticatedUser owner = registerAndAuthenticateUser();
        AuthenticatedUser otherUser = registerAndAuthenticateUser();
        UUID ownerId = userLookupApi.getUserByEmail(owner.email()).id();
        UserAvatarUpload upload = userAvatarUploadRepository.save(upload(
                ownerId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.PENDING_UPLOAD,
                Instant.parse("2026-06-28T11:00:00Z"),
                Instant.parse("2026-06-28T11:05:00Z")));

        given(authenticatedJsonSpec(BASE_PATH, otherUser.accessToken()))
                .delete("/avatar/uploads/{uploadId}", upload.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(userAvatarUploadRepository.findById(upload.getId()))
                .get()
                .matches(saved -> saved.getStatus() == UserAvatarUploadStatus.PENDING_UPLOAD)
                .matches(saved -> saved.getSupersededAt() == null);
    }

    @Test
    @DisplayName("Should ignore cancellation for an active ready avatar upload")
    void shouldIgnoreCancellationForActiveReadyAvatarUpload() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        UserAvatarUpload upload = userAvatarUploadRepository.save(upload(
                userId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.READY,
                Instant.parse("2026-06-28T11:00:00Z"),
                Instant.parse("2026-06-28T11:05:00Z")));
        upload.setActive(true);
        upload.setProcessedBucket("iced-latte-users");
        upload.setProcessedKey("avatars/processed/%s/%s/avatar-384.webp".formatted(userId, upload.getId()));
        upload.setProcessedAt(Instant.parse("2026-06-28T11:02:00Z"));
        userAvatarUploadRepository.save(upload);

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .delete("/avatar/uploads/{uploadId}", upload.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(userAvatarUploadRepository.findById(upload.getId()))
                .get()
                .matches(saved -> saved.getStatus() == UserAvatarUploadStatus.READY)
                .matches(UserAvatarUpload::isActive)
                .matches(saved -> saved.getSupersededAt() == null);
    }

    @Test
    @DisplayName("Should return avatar upload status for the owning user")
    void shouldReturnAvatarUploadStatusForOwningUser() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        UserAvatarUpload upload = userAvatarUploadRepository.save(upload(
                userId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.FAILED,
                Instant.parse("2026-06-28T11:00:00Z"),
                Instant.parse("2026-06-28T11:05:00Z")));

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .accept(ContentType.JSON)
                .get("/avatar/uploads/{uploadId}", upload.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("uploadId", equalTo(upload.getId().toString()))
                .body("status", equalTo("FAILED"))
                .body("failureCode", equalTo("DECODE_FAILED"))
                .body("expiresAt", notNullValue());
    }

    @Test
    @DisplayName("Should hide avatar upload status from another user")
    void shouldHideAvatarUploadStatusFromAnotherUser() {
        AuthenticatedUser owner = registerAndAuthenticateUser();
        AuthenticatedUser otherUser = registerAndAuthenticateUser();
        UUID ownerId = userLookupApi.getUserByEmail(owner.email()).id();
        UserAvatarUpload upload = userAvatarUploadRepository.save(upload(
                ownerId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.PENDING_UPLOAD,
                Instant.parse("2026-06-28T11:00:00Z"),
                Instant.parse("2026-06-28T11:05:00Z")));

        given(authenticatedJsonSpec(BASE_PATH, otherUser.accessToken()))
                .accept(ContentType.JSON)
                .get("/avatar/uploads/{uploadId}", upload.getId())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should supersede pending and ready avatar uploads when deleting avatar")
    void shouldSupersedePendingAndReadyAvatarUploadsWhenDeletingAvatar() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        UserAvatarUpload pending = userAvatarUploadRepository.save(upload(
                userId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.PENDING_UPLOAD,
                Instant.parse("2026-06-28T11:00:00Z"),
                Instant.parse("2026-06-28T11:05:00Z")));
        UserAvatarUpload ready = userAvatarUploadRepository.save(upload(
                userId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.READY,
                Instant.parse("2026-06-28T11:01:00Z"),
                Instant.parse("2026-06-28T11:06:00Z")));
        ready.setActive(true);
        ready.setProcessedBucket("iced-latte-users");
        ready.setProcessedKey("avatars/processed/%s/%s/avatar-384.webp".formatted(userId, ready.getId()));
        ready.setProcessedAt(Instant.parse("2026-06-28T11:02:00Z"));
        userAvatarUploadRepository.save(ready);

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .accept(ContentType.JSON)
                .delete("/avatar")
                .then()
                .statusCode(HttpStatus.OK.value());

        List<UserAvatarUpload> uploads = uploadsForUser(userId);
        assertThat(uploads).hasSize(2);
        assertThat(uploads).allSatisfy(saved -> {
            assertThat(saved.getStatus()).isEqualTo(UserAvatarUploadStatus.SUPERSEDED);
            assertThat(saved.isActive()).isFalse();
            assertThat(saved.getSupersededAt()).isNotNull();
        });

        assertThat(userAvatarUploadRepository.findById(pending.getId()))
                .get()
                .matches(saved -> saved.getStatus() == UserAvatarUploadStatus.SUPERSEDED);
        assertThat(userAvatarUploadRepository.findById(ready.getId()))
                .get()
                .matches(saved -> saved.getStatus() == UserAvatarUploadStatus.SUPERSEDED);
    }

    private List<UserAvatarUpload> uploadsForUser(UUID userId) {
        return userAvatarUploadRepository.findAll().stream()
                .filter(upload -> upload.getUserId().equals(userId))
                .toList();
    }

    private static UserAvatarUpload upload(
            UUID userId, UUID uploadId, UserAvatarUploadStatus status, Instant createdAt, Instant expiresAt) {
        return UserAvatarUpload.builder()
                .id(uploadId)
                .userId(userId)
                .status(status)
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/%s/source".formatted(userId, uploadId))
                .contentType("image/png")
                .originalSizeBytes(1024L)
                .clientIdempotencyKey("avatar-key-" + uploadId)
                .failureCode(status == UserAvatarUploadStatus.FAILED ? "DECODE_FAILED" : null)
                .failureMessage(status == UserAvatarUploadStatus.FAILED ? "Cannot decode image" : null)
                .active(false)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
    }
}

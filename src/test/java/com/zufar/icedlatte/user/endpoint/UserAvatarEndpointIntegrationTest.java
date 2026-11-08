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

    @Autowired
    private com.zufar.icedlatte.user.service.AvatarUploadLifecycleService uploadLifecycle;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private com.zufar.icedlatte.filestorage.api.FileStorageWriterApi storageWriter;

    @Test
    void avatarWritesAdvanceProfileRevisionsAndRejectedUploadsPreserveTheCurrentPhoto() {
        var user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        org.mockito.Mockito.when(objectStorage.isConfigured()).thenReturn(true);
        org.mockito.Mockito.when(objectStorage.getUrl(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> java.util.Optional.of("https://storage.example.com/"
                        + ((com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto) invocation.getArgument(0))
                                .fileName()));
        long before = profileRevision(user.accessToken());
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
        for (int i = 1; i <= 2; i++) {
            given().port(port)
                    .basePath(BASE_PATH)
                    .header("Authorization", "Bearer " + user.accessToken())
                    .multiPart("file", "avatar.png", png, "image/png")
                    .post("/avatar")
                    .then()
                    .statusCode(200);
            assertThat(profileRevision(user.accessToken())).isEqualTo(before + i);
        }
        String current = given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .get("/avatar")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .multiPart("file", "bad.jpg", "not a jpeg".getBytes(), "image/jpeg")
                .post("/avatar")
                .then()
                .statusCode(400);
        assertThat(profileRevision(user.accessToken())).isEqualTo(before + 2);
        assertThat(given().port(port)
                        .basePath(BASE_PATH)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .get("/avatar")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString())
                .isEqualTo(current);
        given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .delete("/avatar")
                .then()
                .statusCode(200);
        assertThat(profileRevision(user.accessToken())).isEqualTo(before + 3);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM file_metadata WHERE related_object_id = ?", Long.class, userId))
                .isZero();
    }

    private long profileRevision(String token) {
        return given().port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + token)
                .get()
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("profileRevision");
    }

    @Test
    void replayedTerminalCompletionSchedulesDeletionEvenAfterAnOlderDeletionWasPublished() {
        var user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        var failed = upload(
                userId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.FAILED,
                Instant.now().minusSeconds(120),
                Instant.now().plusSeconds(120));
        userAvatarUploadRepository.saveAndFlush(failed);
        String key = "avatars/processed/%s/%s/avatar.webp".formatted(userId, failed.getId());
        storageWriter.enqueueDeleteObject(
                new com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto(failed.getId(), "iced-latte-users", key));
        jdbc.update(
                "UPDATE outbox_events SET status = 'PUBLISHED' WHERE event_type = 'file.object.delete' AND payload ->> 'fileName' = ?",
                key);
        var source = new com.zufar.icedlatte.user.service.ValidAvatarUploadSourceObject(
                failed.getOriginalBucket(), failed.getOriginalKey(), userId, failed.getId(), "image/png");
        var completion = new com.zufar.icedlatte.user.service.AvatarUploadCompletion(
                source, "iced-latte-users", key, "image/webp", 10, 10, 1024, 512, "0".repeat(64));
        assertThat(uploadLifecycle.markReady(completion)).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_events WHERE event_type = 'file.object.delete' AND status = 'PENDING' AND payload ->> 'fileName' = ?",
                        Long.class,
                        key))
                .isEqualTo(1);
    }

    @Test
    void concurrentNewLinksCannotBypassTheLimitBySupersedingOneAnother() throws Exception {
        var user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var calls = java.util.stream.IntStream.range(0, 12)
                    .mapToObj(i -> (java.util.concurrent.Callable<Boolean>) () -> {
                        try {
                            uploadLifecycle.createPendingUpload(userId, "image/png", 1024, "concurrent-" + i);
                            return true;
                        } catch (org.springframework.web.server.ResponseStatusException ex) {
                            assertThat(ex.getStatusCode().value()).isEqualTo(429);
                            return false;
                        }
                    })
                    .toList();
            int accepted = 0;
            for (var result : pool.invokeAll(calls)) if (result.get()) accepted++;
            assertThat(accepted).isEqualTo(3);
        }
        var pending = userAvatarUploadRepository.findByUserIdAndStatusIn(
                userId, List.of(UserAvatarUploadStatus.PENDING_UPLOAD));
        assertThat(pending).hasSize(1);
        var latest = pending.iterator().next();
        assertThat(uploadLifecycle
                        .createPendingUpload(
                                userId,
                                latest.getContentType(),
                                latest.getOriginalSizeBytes(),
                                latest.getClientIdempotencyKey())
                        .getId())
                .isEqualTo(latest.getId());
        assertThat(jdbc.queryForObject(
                        "SELECT used FROM daily_operation_quota WHERE scope = ?", Long.class, "avatar:user:" + userId))
                .isEqualTo(3);
    }

    @Test
    void finalCleanupRepairsOldSourceDeletionWithoutDeletingTheActiveAvatar() {
        var user = registerAndAuthenticateUser();
        UUID userId = userLookupApi.getUserByEmail(user.email()).id();
        var ready = upload(
                userId,
                UUID.randomUUID(),
                UserAvatarUploadStatus.READY,
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600));
        ready.setActive(true);
        ready.setProcessedBucket("iced-latte-users");
        ready.setProcessedKey("avatars/processed/%s/%s/avatar.webp".formatted(userId, ready.getId()));
        userAvatarUploadRepository.saveAndFlush(ready);
        uploadLifecycle.expireStaleUploads(Instant.now());
        var after = userAvatarUploadRepository.findById(ready.getId()).orElseThrow();
        assertThat(after.isActive()).isTrue();
        assertThat(after.getStatus()).isEqualTo(UserAvatarUploadStatus.READY);
        assertThat(after.isSourceCleanupScheduled()).isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_events WHERE event_type = 'file.object.delete' AND payload ->> 'fileName' = ?",
                        Long.class,
                        ready.getOriginalKey()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_events WHERE event_type = 'file.object.delete' AND payload ->> 'fileName' = ?",
                        Long.class,
                        ready.getProcessedKey()))
                .isZero();
    }

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

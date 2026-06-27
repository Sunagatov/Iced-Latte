package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zufar.icedlatte.openapi.dto.AvatarUploadTargetResponse;
import com.zufar.icedlatte.user.config.AvatarUploadMode;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;
import com.zufar.icedlatte.user.exception.UserAvatarUploadException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@DisplayName("AwsAvatarUploadPresigner unit tests")
class AwsAvatarUploadPresignerTest {

    @Test
    @DisplayName("creates presigned PUT target for avatar upload")
    void presignCreatesPutTarget() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(s3Presigner.presignPutObject(org.mockito.ArgumentMatchers.any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URL("https://uploads.example.test/avatar"));
        when(presignedRequest.signedHeaders())
                .thenReturn(Map.of(
                        "content-type", List.of("image/png"),
                        "x-amz-meta-upload-id", List.of("b67665b0-e1b8-4a58-b1f7-c71fd32a9431"),
                        "x-amz-meta-user-id", List.of("a4b55d63-f743-4154-a21e-aae91dba0b99"),
                        "x-amz-meta-requested-content-type", List.of("image/png"),
                        "x-amz-meta-avatar-upload-version", List.of("1"),
                        "host", List.of("uploads.example.test")));

        AwsAvatarUploadPresigner presigner = new AwsAvatarUploadPresigner(s3Presigner, properties());
        UserAvatarUpload upload = upload();

        AvatarUploadTargetResponse response = presigner.presign(upload);

        assertThat(response.getMethod()).isEqualTo(AvatarUploadTargetResponse.MethodEnum.PUT);
        assertThat(response.getUrl()).hasToString("https://uploads.example.test/avatar");
        assertThat(response.getHeaders())
                .containsEntry("content-type", "image/png")
                .containsEntry("x-amz-meta-upload-id", "b67665b0-e1b8-4a58-b1f7-c71fd32a9431")
                .containsEntry("x-amz-meta-user-id", "a4b55d63-f743-4154-a21e-aae91dba0b99")
                .containsEntry("x-amz-meta-requested-content-type", "image/png")
                .containsEntry("x-amz-meta-avatar-upload-version", "1")
                .doesNotContainKey("host");
        assertThat(response.getFields()).isEmpty();

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        org.mockito.Mockito.verify(s3Presigner).presignPutObject(captor.capture());
        PutObjectRequest putObjectRequest = captor.getValue().putObjectRequest();
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(putObjectRequest.bucket()).isEqualTo("iced-latte-users");
        assertThat(putObjectRequest.key())
                .isEqualTo(
                        "avatars/incoming/a4b55d63-f743-4154-a21e-aae91dba0b99/b67665b0-e1b8-4a58-b1f7-c71fd32a9431/source");
        assertThat(putObjectRequest.contentType()).isEqualTo("image/png");
        assertThat(putObjectRequest.metadata())
                .containsEntry("upload-id", "b67665b0-e1b8-4a58-b1f7-c71fd32a9431")
                .containsEntry("user-id", "a4b55d63-f743-4154-a21e-aae91dba0b99")
                .containsEntry("requested-content-type", "image/png")
                .containsEntry("avatar-upload-version", "1");
    }

    @Test
    @DisplayName("maps S3 presign failure to user avatar upload exception")
    void presignMapsS3FailureToUserAvatarUploadException() {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        when(s3Presigner.presignPutObject(org.mockito.ArgumentMatchers.any(PutObjectPresignRequest.class)))
                .thenThrow(
                        SdkClientException.builder().message("S3 unavailable").build());

        AwsAvatarUploadPresigner presigner = new AwsAvatarUploadPresigner(s3Presigner, properties());

        assertThatThrownBy(() -> presigner.presign(upload()))
                .isInstanceOf(UserAvatarUploadException.class)
                .hasMessageContaining("userId=a4b55d63-f743-4154-a21e-aae91dba0b99");
    }

    private static UserAvatarUpload upload() {
        return UserAvatarUpload.builder()
                .id(UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431"))
                .userId(UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99"))
                .status(UserAvatarUploadStatus.PENDING_UPLOAD)
                .originalBucket("iced-latte-users")
                .originalKey(
                        "avatars/incoming/a4b55d63-f743-4154-a21e-aae91dba0b99/b67665b0-e1b8-4a58-b1f7-c71fd32a9431/source")
                .contentType("image/png")
                .originalSizeBytes(1024L)
                .createdAt(Instant.parse("2026-06-27T10:15:30Z"))
                .expiresAt(Instant.parse("2026-06-27T10:20:30Z"))
                .active(false)
                .build();
    }

    private static AvatarUploadProperties properties() {
        return new AvatarUploadProperties(
                AvatarUploadMode.PRESIGNED,
                Duration.ofMinutes(5),
                5_242_880L,
                12_000_000L,
                Duration.ofMinutes(10),
                "iced-latte-users",
                "iced-latte-users",
                "");
    }
}

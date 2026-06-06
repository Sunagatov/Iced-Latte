package com.zufar.icedlatte.filestorage.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.exception.FileListException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsObjectStorage")
class AwsObjectStorageTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("public URL includes bucket when base URL does not")
    void publicUrlIncludesBucketWhenBaseUrlDoesNot() {
        AwsObjectStorage storage = new AwsObjectStorage(s3Client, s3Presigner, properties("https://cdn.example.com"));
        UUID relatedObjectId = UUID.randomUUID();

        assertThat(storage.getUrl(new FileMetadataDto(
                        relatedObjectId, "products", "Latte " + relatedObjectId + "/card logo.png")))
                .contains("https://cdn.example.com/products/Latte%20" + relatedObjectId + "/card%20logo.png");
    }

    @Test
    @DisplayName("public URL does not duplicate bucket when base URL already includes it")
    void publicUrlDoesNotDuplicateBucket() {
        AwsObjectStorage storage =
                new AwsObjectStorage(s3Client, s3Presigner, properties("https://cdn.example.com/products"));

        assertThat(storage.getUrl(new FileMetadataDto(UUID.randomUUID(), "products", "card_logo.webp")))
                .contains("https://cdn.example.com/products/card_logo.webp");
    }

    @Test
    @DisplayName("Supabase public URL base for product bucket falls back to signed URL for avatar bucket")
    void supabaseProductPublicUrlBaseFallsBackToSignedUrlForAvatarBucket() throws Exception {
        AwsObjectStorage storage = new AwsObjectStorage(
                s3Client,
                s3Presigner,
                properties("https://project.supabase.co/storage/v1/object/public/iced-latte-products"));
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url())
                .thenReturn(URI.create("https://signed.example.com/avatar").toURL());
        when(s3Presigner.presignGetObject(anyConsumer())).thenReturn(presignedRequest);

        assertThat(storage.getUrl(new FileMetadataDto(UUID.randomUUID(), "iced-latte-users", "avatar.png")))
                .contains("https://signed.example.com/avatar");
    }

    @Test
    @DisplayName("Supabase public URL base without bucket appends requested bucket")
    void supabasePublicUrlBaseWithoutBucketAppendsRequestedBucket() {
        AwsObjectStorage storage = new AwsObjectStorage(
                s3Client, s3Presigner, properties("https://project.supabase.co/storage/v1/object/public"));

        assertThat(storage.getUrl(new FileMetadataDto(UUID.randomUUID(), "iced-latte-users", "avatar.png")))
                .contains("https://project.supabase.co/storage/v1/object/public/iced-latte-users/avatar.png");
    }

    @Test
    @DisplayName("listObjectKeys throws when S3 listing fails")
    void listObjectKeysThrowsWhenS3ListingFails() {
        AwsObjectStorage storage = new AwsObjectStorage(s3Client, s3Presigner, properties(""));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("storage down").build());

        assertThatThrownBy(() -> storage.listObjectKeys("products")).isInstanceOf(FileListException.class);
    }

    @Test
    @DisplayName("uploadDirectory uploads regular files with relative object keys")
    void uploadDirectoryUploadsRegularFilesWithRelativeObjectKeys() throws IOException {
        AwsObjectStorage storage = new AwsObjectStorage(s3Client, s3Presigner, properties(""));
        Path nestedDir = Files.createDirectory(tempDir.resolve("nested"));
        Files.writeString(nestedDir.resolve("card_logo.png"), "image");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storage.uploadDirectory("products", tempDir.toString());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("products");
        assertThat(requestCaptor.getValue().key()).isEqualTo("nested/card_logo.png");
    }

    @Test
    @DisplayName("uploadDirectory rejects non-directory paths")
    void uploadDirectoryRejectsNonDirectoryPaths() throws IOException {
        AwsObjectStorage storage = new AwsObjectStorage(s3Client, s3Presigner, properties(""));
        Path file = Files.writeString(tempDir.resolve("not-a-directory.txt"), "image");

        assertThatThrownBy(() -> storage.uploadDirectory("products", file.toString()))
                .isInstanceOf(BadRequestException.class);
    }

    private static AwsProperties properties(String publicUrlBase) {
        return new AwsProperties(
                "access-key",
                "secret-key",
                "eu-west-2",
                "",
                publicUrlBase,
                "",
                Duration.ofHours(1),
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(60),
                Duration.ofSeconds(15));
    }

    private static Consumer<GetObjectPresignRequest.Builder> anyConsumer() {
        return any();
    }
}

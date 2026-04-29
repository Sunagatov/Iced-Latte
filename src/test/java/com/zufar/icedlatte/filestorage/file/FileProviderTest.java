package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsTemporaryLinkReceiver;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("FileProvider")
class FileProviderTest {

    private final AwsTemporaryLinkReceiver awsTemporaryLinkReceiver = mock(AwsTemporaryLinkReceiver.class);
    private final FileMetadataProvider fileMetadataProvider = mock(FileMetadataProvider.class);

    @Test
    @DisplayName("returns presigned URL when metadata exists and AWS is configured")
    void returnsPresignedUrlWhenMetadataExistsAndAwsConfigured() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto dto = new FileMetadataDto(relatedObjectId, "avatars", "user.png");
        when(fileMetadataProvider.getFileMetadataDto(relatedObjectId)).thenReturn(Optional.of(dto));
        when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(dto)).thenReturn("https://cdn.example.com/user.png");

        Optional<String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider)
                .getRelatedObjectUrl(relatedObjectId);

        assertThat(result).contains("https://cdn.example.com/user.png");
        verify(fileMetadataProvider).getFileMetadataDto(relatedObjectId);
    }

    @Test
    @DisplayName("returns empty when AWS is disabled")
    void returnsEmptyWhenAwsDisabled() {
        Optional<String> result = new FileProvider(null, fileMetadataProvider)
                .getRelatedObjectUrl(UUID.randomUUID());

        assertThat(result).isEmpty();
        verifyNoInteractions(fileMetadataProvider);
    }

    @Test
    @DisplayName("returns empty when metadata is missing or URL generation returns null")
    void returnsEmptyWhenMetadataMissingOrUrlGenerationReturnsNull() {
        UUID missingId = UUID.randomUUID();
        UUID nullUrlId = UUID.randomUUID();
        FileMetadataDto dto = new FileMetadataDto(nullUrlId, "avatars", "user.png");
        when(fileMetadataProvider.getFileMetadataDto(missingId)).thenReturn(Optional.empty());
        when(fileMetadataProvider.getFileMetadataDto(nullUrlId)).thenReturn(Optional.of(dto));
        when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(dto)).thenReturn(null);

        FileProvider provider = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider);

        assertThat(provider.getRelatedObjectUrl(missingId)).isEmpty();
        assertThat(provider.getRelatedObjectUrl(nullUrlId)).isEmpty();
    }

    @Test
    @DisplayName("bulk lookup returns only entries with generated URLs")
    void bulkLookupReturnsOnlyEntriesWithGeneratedUrls() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        FileMetadataDto firstDto = new FileMetadataDto(firstId, "avatars", "first.png");
        FileMetadataDto secondDto = new FileMetadataDto(secondId, "avatars", "second.png");
        when(fileMetadataProvider.getFileMetadataDtos(List.of(firstId, secondId)))
                .thenReturn(Map.of(firstId, firstDto, secondId, secondDto));
        when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(firstDto)).thenReturn("https://cdn.example.com/first.png");
        when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(secondDto)).thenReturn(null);

        Map<UUID, String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider)
                .getRelatedObjectUrls(List.of(firstId, secondId));

        assertThat(result).containsExactly(Map.entry(firstId, "https://cdn.example.com/first.png"));
    }
}

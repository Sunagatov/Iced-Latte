package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsProvider unit tests")
class AwsProviderTest {

    @Mock private S3Client s3Client;
    @Mock private ListObjectsV2Iterable paginator;

    @InjectMocks private AwsProvider provider;

    @Test
    @DisplayName("returns metadata only for keys with a valid UUID package prefix")
    void returnsMetadataOnlyForKeysWithValidUuidPackagePrefix() {
        UUID productId = UUID.randomUUID();
        List<S3Object> objects = List.of(
                S3Object.builder().key("product_" + productId + "/main.jpg").build(),
                S3Object.builder().key("product-not-separated/main.jpg").build(),
                S3Object.builder().key("product_not-a-uuid/main.jpg").build()
        );

        when(s3Client.listObjectsV2Paginator(org.mockito.ArgumentMatchers.any(ListObjectsV2Request.class)))
                .thenReturn(paginator);
        when(paginator.contents()).thenReturn(iterable(objects));

        List<FileMetadataDto> result = provider.getProductImagesFromAWS("bucket-name");

        assertThat(result).containsExactly(new FileMetadataDto(productId, "bucket-name", "product_" + productId + "/main.jpg"));

        ArgumentCaptor<ListObjectsV2Request> requestCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2Paginator(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("bucket-name");
    }

    @Test
    @DisplayName("returns an empty list when S3 listing fails")
    void returnsEmptyListWhenS3ListingFails() {
        when(s3Client.listObjectsV2Paginator(org.mockito.ArgumentMatchers.any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        assertThat(provider.getProductImagesFromAWS("bucket-name")).isEmpty();
    }

    @Test
    @DisplayName("returns an empty list when the AWS client is unreachable")
    void returnsEmptyListWhenAwsClientIsUnreachable() {
        when(s3Client.listObjectsV2Paginator(org.mockito.ArgumentMatchers.any(ListObjectsV2Request.class)))
                .thenThrow(SdkClientException.create("offline"));

        assertThat(provider.getProductImagesFromAWS("bucket-name")).isEmpty();
    }

    private static SdkIterable<S3Object> iterable(List<S3Object> objects) {
        return objects::iterator;
    }
}

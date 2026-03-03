package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductImageReceiver unit tests")
class ProductImageReceiverTest {

    private static final String PLACEHOLDER = "/assets/images/product-placeholder.png";

    @Mock
    private FileProvider fileProvider;
    @InjectMocks
    private ProductImageReceiver receiver;

    @Test
    @DisplayName("Returns URL when FileProvider has one")
    void getProductFileUrl_found_returnsUrl() {
        UUID id = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrl(id)).thenReturn(Optional.of("https://cdn.example.com/img.jpg"));

        assertThat(receiver.getProductFileUrl(id)).isEqualTo("https://cdn.example.com/img.jpg");
    }

    @Test
    @DisplayName("Returns placeholder when FileProvider returns empty")
    void getProductFileUrl_notFound_returnsPlaceholder() {
        UUID id = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrl(id)).thenReturn(Optional.empty());

        assertThat(receiver.getProductFileUrl(id)).isEqualTo(PLACEHOLDER);
    }

    @Test
    @DisplayName("Returns placeholder when FileProvider throws RuntimeException")
    void getProductFileUrl_exception_returnsPlaceholder() {
        UUID id = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrl(id)).thenThrow(new RuntimeException("S3 down"));

        assertThat(receiver.getProductFileUrl(id)).isEqualTo(PLACEHOLDER);
    }

    @Test
    @DisplayName("getProductFileUrls fills placeholder for IDs missing from FileProvider result")
    void getProductFileUrls_partialResult_fillsPlaceholder() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrls(List.of(id1, id2)))
                .thenReturn(Map.of(id1, "https://cdn.example.com/img1.jpg"));

        Map<UUID, String> result = receiver.getProductFileUrls(List.of(id1, id2));

        assertThat(result.get(id1)).isEqualTo("https://cdn.example.com/img1.jpg");
        assertThat(result.get(id2)).isEqualTo(PLACEHOLDER);
    }

    @Test
    @DisplayName("getProductFileUrls returns all placeholders when FileProvider throws")
    void getProductFileUrls_exception_allPlaceholders() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrls(List.of(id1, id2))).thenThrow(new RuntimeException("S3 down"));

        Map<UUID, String> result = receiver.getProductFileUrls(List.of(id1, id2));

        assertThat(result).containsEntry(id1, PLACEHOLDER).containsEntry(id2, PLACEHOLDER);
    }
}

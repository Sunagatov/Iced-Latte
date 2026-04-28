package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import com.zufar.icedlatte.product.entity.ProductImage;
import com.zufar.icedlatte.product.repository.ProductImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductImageReceiver unit tests")
class ProductImageReceiverTest {

    private static final String PLACEHOLDER = "/assets/images/product-placeholder.png";

    @Mock private FileProvider fileProvider;
    @Mock private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductImageReceiver receiver;

    @Nested
    @DisplayName("getProductFileUrl")
    class GetProductFileUrl {

        @Test
        @DisplayName("returns the provider URL when present")
        void returnsProviderUrlWhenPresent() {
            UUID productId = UUID.randomUUID();
            when(fileProvider.getRelatedObjectUrl(productId)).thenReturn(Optional.of("https://cdn.example.com/hero.jpg"));

            assertThat(receiver.getProductFileUrl(productId)).isEqualTo("https://cdn.example.com/hero.jpg");
            verify(fileProvider).getRelatedObjectUrl(productId);
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }

        @Test
        @DisplayName("returns the placeholder when the provider has no URL")
        void returnsPlaceholderWhenProviderHasNoUrl() {
            UUID productId = UUID.randomUUID();
            when(fileProvider.getRelatedObjectUrl(productId)).thenReturn(Optional.empty());

            assertThat(receiver.getProductFileUrl(productId)).isEqualTo(PLACEHOLDER);
            verify(fileProvider).getRelatedObjectUrl(productId);
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }

        @Test
        @DisplayName("returns the placeholder when the provider throws")
        void returnsPlaceholderWhenProviderThrows() {
            UUID productId = UUID.randomUUID();
            when(fileProvider.getRelatedObjectUrl(productId)).thenThrow(new RuntimeException("S3 down"));

            assertThat(receiver.getProductFileUrl(productId)).isEqualTo(PLACEHOLDER);
            verify(fileProvider).getRelatedObjectUrl(productId);
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }
    }

    @Nested
    @DisplayName("getProductImageUrls")
    class GetProductImageUrls {

        @Test
        @DisplayName("returns repository image URLs in repository order")
        void returnsRepositoryImageUrlsInRepositoryOrder() {
            UUID productId = UUID.randomUUID();
            when(productImageRepository.findByProductIdOrderByPosition(productId)).thenReturn(List.of(
                    new ProductImage(UUID.randomUUID(), productId, "url-1", (short) 1),
                    new ProductImage(UUID.randomUUID(), productId, "url-2", (short) 2)
            ));

            List<String> result = receiver.getProductImageUrls(productId);

            assertThat(result).containsExactly("url-1", "url-2");
            verify(productImageRepository).findByProductIdOrderByPosition(productId);
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }

        @Test
        @DisplayName("returns an empty list when the repository has no images")
        void returnsEmptyListWhenRepositoryHasNoImages() {
            UUID productId = UUID.randomUUID();
            when(productImageRepository.findByProductIdOrderByPosition(productId)).thenReturn(List.of());

            assertThat(receiver.getProductImageUrls(productId)).isEmpty();
            verify(productImageRepository).findByProductIdOrderByPosition(productId);
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }
    }

    @Nested
    @DisplayName("getProductImageUrlsBatch")
    class GetProductImageUrlsBatch {

        @Test
        @DisplayName("groups image URLs by product id in repository order")
        void groupsImageUrlsByProductIdInRepositoryOrder() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            when(productImageRepository.findByProductIdInOrderByPosition(List.of(productId1, productId2))).thenReturn(List.of(
                    new ProductImage(UUID.randomUUID(), productId1, "p1-1", (short) 1),
                    new ProductImage(UUID.randomUUID(), productId1, "p1-2", (short) 2),
                    new ProductImage(UUID.randomUUID(), productId2, "p2-1", (short) 1)
            ));

            Map<UUID, List<String>> result = receiver.getProductImageUrlsBatch(List.of(productId1, productId2));

            assertThat(result).containsEntry(productId1, List.of("p1-1", "p1-2"));
            assertThat(result).containsEntry(productId2, List.of("p2-1"));
            verify(productImageRepository).findByProductIdInOrderByPosition(List.of(productId1, productId2));
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }
    }

    @Nested
    @DisplayName("getProductFileUrls")
    class GetProductFileUrls {

        @Test
        @DisplayName("fills placeholders for products missing from the provider result")
        void fillsPlaceholdersForProductsMissingFromProviderResult() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            when(fileProvider.getRelatedObjectUrls(List.of(productId1, productId2)))
                    .thenReturn(Map.of(productId1, "https://cdn.example.com/img1.jpg"));

            Map<UUID, String> result = receiver.getProductFileUrls(List.of(productId1, productId2));

            assertThat(result).containsEntry(productId1, "https://cdn.example.com/img1.jpg");
            assertThat(result).containsEntry(productId2, PLACEHOLDER);
            verify(fileProvider).getRelatedObjectUrls(List.of(productId1, productId2));
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }

        @Test
        @DisplayName("returns placeholders for all products when the provider throws")
        void returnsPlaceholdersForAllProductsWhenProviderThrows() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            when(fileProvider.getRelatedObjectUrls(List.of(productId1, productId2))).thenThrow(new RuntimeException("S3 down"));

            Map<UUID, String> result = receiver.getProductFileUrls(List.of(productId1, productId2));

            assertThat(result).containsEntry(productId1, PLACEHOLDER);
            assertThat(result).containsEntry(productId2, PLACEHOLDER);
            verify(fileProvider).getRelatedObjectUrls(List.of(productId1, productId2));
            verifyNoMoreInteractions(fileProvider, productImageRepository);
        }
    }
}

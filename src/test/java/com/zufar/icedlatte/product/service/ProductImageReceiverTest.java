package com.zufar.icedlatte.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.zufar.icedlatte.filestorage.api.FileUrlResolverApi;
import com.zufar.icedlatte.product.entity.ProductImage;
import com.zufar.icedlatte.product.repository.ProductImageRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductImageReceiver unit tests")
class ProductImageReceiverTest {

    private static final String PLACEHOLDER = "/assets/images/product-placeholder.png";

    @Mock
    private FileUrlResolverApi fileStorageService;

    @Mock
    private ProductImageRepository productImageRepository;

    private ProductImageReceiver receiver;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        receiver = new ProductImageReceiver(fileStorageService, productImageRepository, meterRegistry);
        ReflectionTestUtils.setField(receiver, "placeholderImageUrl", PLACEHOLDER);
    }

    @Nested
    @DisplayName("getProductFileUrl")
    class GetProductFileUrl {

        @Test
        @DisplayName("returns the provider URL when present")
        void returnsProviderUrlWhenPresent() {
            UUID productId = UUID.randomUUID();
            when(fileStorageService.findFileUrl(productId)).thenReturn(Optional.of("https://cdn.example.com/hero.jpg"));

            assertThat(receiver.getProductFileUrl(productId)).isEqualTo("https://cdn.example.com/hero.jpg");
            verify(fileStorageService).findFileUrl(productId);
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }

        @Test
        @DisplayName("returns the placeholder when the provider has no URL")
        void returnsPlaceholderWhenProviderHasNoUrl() {
            UUID productId = UUID.randomUUID();
            when(fileStorageService.findFileUrl(productId)).thenReturn(Optional.empty());

            assertThat(receiver.getProductFileUrl(productId)).isEqualTo(PLACEHOLDER);
            verify(fileStorageService).findFileUrl(productId);
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }

        @Test
        @DisplayName("returns the placeholder when the provider throws")
        void returnsPlaceholderWhenProviderThrows() {
            UUID productId = UUID.randomUUID();
            when(fileStorageService.findFileUrl(productId)).thenThrow(new RuntimeException("S3 down"));

            assertThat(receiver.getProductFileUrl(productId)).isEqualTo(PLACEHOLDER);
            assertThat(meterRegistry
                            .counter("product.image.fallback", "mode", "single")
                            .count())
                    .isEqualTo(1);
            verify(fileStorageService).findFileUrl(productId);
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }
    }

    @Nested
    @DisplayName("getProductImageUrls")
    class GetProductImageUrls {

        @Test
        @DisplayName("returns repository image URLs in repository order")
        void returnsRepositoryImageUrlsInRepositoryOrder() {
            UUID productId = UUID.randomUUID();
            when(productImageRepository.findByProductIdOrderByPositionAscIdAsc(productId))
                    .thenReturn(List.of(
                            new ProductImage(UUID.randomUUID(), productId, "url-1", (short) 1),
                            new ProductImage(UUID.randomUUID(), productId, "url-2", (short) 2)));

            List<String> result = receiver.getProductImageUrls(productId);

            assertThat(result).containsExactly("url-1", "url-2");
            verify(productImageRepository).findByProductIdOrderByPositionAscIdAsc(productId);
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }

        @Test
        @DisplayName("returns an empty list when the repository has no images")
        void returnsEmptyListWhenRepositoryHasNoImages() {
            UUID productId = UUID.randomUUID();
            when(productImageRepository.findByProductIdOrderByPositionAscIdAsc(productId))
                    .thenReturn(List.of());

            assertThat(receiver.getProductImageUrls(productId)).isEmpty();
            verify(productImageRepository).findByProductIdOrderByPositionAscIdAsc(productId);
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }
    }

    @Nested
    @DisplayName("getProductImageUrlsBatch")
    class GetProductImageUrlsBatch {

        @Test
        @DisplayName("returns an empty map for an empty batch without querying the repository")
        void returnsEmptyMapForEmptyBatch() {
            assertThat(receiver.getProductImageUrlsBatch(List.of())).isEmpty();

            verifyNoInteractions(fileStorageService, productImageRepository);
        }

        @Test
        @DisplayName("groups image URLs by product id in repository order")
        void groupsImageUrlsByProductIdInRepositoryOrder() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            when(productImageRepository.findByProductIdInOrderByProductIdAscPositionAscIdAsc(
                            List.of(productId1, productId2)))
                    .thenReturn(List.of(
                            new ProductImage(UUID.randomUUID(), productId1, "p1-1", (short) 1),
                            new ProductImage(UUID.randomUUID(), productId1, "p1-2", (short) 2),
                            new ProductImage(UUID.randomUUID(), productId2, "p2-1", (short) 1)));

            Map<UUID, List<String>> result = receiver.getProductImageUrlsBatch(List.of(productId1, productId2));

            assertThat(result).containsEntry(productId1, List.of("p1-1", "p1-2"));
            assertThat(result).containsEntry(productId2, List.of("p2-1"));
            verify(productImageRepository)
                    .findByProductIdInOrderByProductIdAscPositionAscIdAsc(List.of(productId1, productId2));
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }
    }

    @Nested
    @DisplayName("getProductFileUrls")
    class GetProductFileUrls {

        @Test
        @DisplayName("returns an empty map for an empty batch without calling the provider")
        void returnsEmptyMapForEmptyBatch() {
            assertThat(receiver.getProductFileUrls(List.of())).isEmpty();

            verifyNoInteractions(fileStorageService, productImageRepository);
        }

        @Test
        @DisplayName("fills placeholders for products missing from the provider result")
        void fillsPlaceholdersForProductsMissingFromProviderResult() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            when(fileStorageService.findFileUrls(List.of(productId1, productId2)))
                    .thenReturn(Map.of(productId1, "https://cdn.example.com/img1.jpg"));

            Map<UUID, String> result = receiver.getProductFileUrls(List.of(productId1, productId2));

            assertThat(result).containsEntry(productId1, "https://cdn.example.com/img1.jpg");
            assertThat(result).containsEntry(productId2, PLACEHOLDER);
            verify(fileStorageService).findFileUrls(List.of(productId1, productId2));
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }

        @Test
        @DisplayName("keeps batch URL resolution stable when duplicate product ids are passed")
        void keepsBatchUrlResolutionStableForDuplicateProductIds() {
            UUID productId = UUID.randomUUID();
            when(fileStorageService.findFileUrls(List.of(productId, productId)))
                    .thenReturn(Map.of(productId, "https://cdn.example.com/img.jpg"));

            Map<UUID, String> result = receiver.getProductFileUrls(List.of(productId, productId));

            assertThat(result).containsOnly(Map.entry(productId, "https://cdn.example.com/img.jpg"));
            verify(fileStorageService).findFileUrls(List.of(productId, productId));
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }

        @Test
        @DisplayName("returns placeholders for all products when the provider throws")
        void returnsPlaceholdersForAllProductsWhenProviderThrows() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            when(fileStorageService.findFileUrls(List.of(productId1, productId2)))
                    .thenThrow(new RuntimeException("S3 down"));

            Map<UUID, String> result = receiver.getProductFileUrls(List.of(productId1, productId2));

            assertThat(result).containsEntry(productId1, PLACEHOLDER);
            assertThat(result).containsEntry(productId2, PLACEHOLDER);
            assertThat(meterRegistry
                            .counter("product.image.fallback", "mode", "batch")
                            .count())
                    .isEqualTo(1);
            verify(fileStorageService).findFileUrls(List.of(productId1, productId2));
            verifyNoMoreInteractions(fileStorageService, productImageRepository);
        }
    }
}

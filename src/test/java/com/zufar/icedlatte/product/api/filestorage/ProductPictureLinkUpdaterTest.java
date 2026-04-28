package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductPictureLinkUpdater unit tests")
class ProductPictureLinkUpdaterTest {

    @Mock private ProductImageReceiver productImageReceiver;
    @InjectMocks private ProductPictureLinkUpdater updater;

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("enriches the dto with both primary and gallery image URLs")
        void enrichesDtoWithBothPrimaryAndGalleryImageUrls() {
            UUID productId = UUID.randomUUID();
            ProductInfoDto dto = new ProductInfoDto();
            dto.setId(productId);
            when(productImageReceiver.getProductFileUrl(productId)).thenReturn("https://cdn.example.com/main.jpg");
            when(productImageReceiver.getProductImageUrls(productId)).thenReturn(List.of("https://cdn.example.com/1.jpg"));

            ProductInfoDto result = updater.update(dto);

            assertThat(result).isSameAs(dto);
            assertThat(result.getProductFileUrl()).isEqualTo("https://cdn.example.com/main.jpg");
            assertThat(result.getProductImageUrls()).containsExactly("https://cdn.example.com/1.jpg");
            verify(productImageReceiver).getProductFileUrl(productId);
            verify(productImageReceiver).getProductImageUrls(productId);
            verifyNoMoreInteractions(productImageReceiver);
        }
    }

    @Nested
    @DisplayName("updateBatch")
    class UpdateBatch {

        @Test
        @DisplayName("enriches every dto from the batch lookups")
        void enrichesEveryDtoFromBatchLookups() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            ProductInfoDto first = new ProductInfoDto();
            first.setId(productId1);
            ProductInfoDto second = new ProductInfoDto();
            second.setId(productId2);

            when(productImageReceiver.getProductFileUrls(List.of(productId1, productId2)))
                    .thenReturn(Map.of(productId1, "url-1", productId2, "url-2"));
            when(productImageReceiver.getProductImageUrlsBatch(List.of(productId1, productId2)))
                    .thenReturn(Map.of(productId1, List.of("gallery-1"), productId2, List.of("gallery-2")));

            List<ProductInfoDto> result = updater.updateBatch(List.of(first, second));

            assertThat(result).containsExactly(first, second);
            assertThat(first.getProductFileUrl()).isEqualTo("url-1");
            assertThat(first.getProductImageUrls()).containsExactly("gallery-1");
            assertThat(second.getProductFileUrl()).isEqualTo("url-2");
            assertThat(second.getProductImageUrls()).containsExactly("gallery-2");
            verify(productImageReceiver).getProductFileUrls(List.of(productId1, productId2));
            verify(productImageReceiver).getProductImageUrlsBatch(List.of(productId1, productId2));
            verifyNoMoreInteractions(productImageReceiver);
        }

        @Test
        @DisplayName("uses empty gallery URLs when a product is missing from the batch image lookup")
        void usesEmptyGalleryUrlsWhenProductIsMissingFromBatchImageLookup() {
            UUID productId = UUID.randomUUID();
            ProductInfoDto dto = new ProductInfoDto();
            dto.setId(productId);

            when(productImageReceiver.getProductFileUrls(List.of(productId)))
                    .thenReturn(Map.of(productId, "main-url"));
            when(productImageReceiver.getProductImageUrlsBatch(List.of(productId)))
                    .thenReturn(Map.of());

            List<ProductInfoDto> result = updater.updateBatch(List.of(dto));

            assertThat(result).containsExactly(dto);
            assertThat(dto.getProductFileUrl()).isEqualTo("main-url");
            assertThat(dto.getProductImageUrls()).isEmpty();
            verify(productImageReceiver).getProductFileUrls(List.of(productId));
            verify(productImageReceiver).getProductImageUrlsBatch(List.of(productId));
            verifyNoMoreInteractions(productImageReceiver);
        }
    }
}

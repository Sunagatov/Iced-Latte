package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SingleProductProvider unit tests")
class SingleProductProviderTest {

    @Mock private ProductInfoRepository productInfoRepository;
    @Mock private ProductInfoDtoConverter productInfoDtoConverter;
    @Mock private ProductPictureLinkUpdater productPictureLinkUpdater;

    @InjectMocks
    private SingleProductProvider provider;

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("loads the product, converts it, and enriches image links")
        void loadsConvertsAndEnrichesProduct() {
            UUID productId = UUID.randomUUID();
            ProductInfo product = new ProductInfo();
            ProductInfoDto converted = new ProductInfoDto();
            ProductInfoDto enriched = new ProductInfoDto();

            when(productInfoRepository.findById(productId)).thenReturn(Optional.of(product));
            when(productInfoDtoConverter.toDto(product)).thenReturn(converted);
            when(productPictureLinkUpdater.update(converted)).thenReturn(enriched);

            ProductInfoDto result = provider.getProductById(productId);

            assertThat(result).isSameAs(enriched);
            verify(productInfoRepository).findById(productId);
            verify(productInfoDtoConverter).toDto(product);
            verify(productPictureLinkUpdater).update(converted);
            verifyNoMoreInteractions(productInfoRepository, productInfoDtoConverter, productPictureLinkUpdater);
        }

        @Test
        @DisplayName("throws ProductNotFoundException when the product does not exist")
        void throwsProductNotFoundExceptionWhenProductDoesNotExist() {
            UUID productId = UUID.randomUUID();
            when(productInfoRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> provider.getProductById(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining(productId.toString());

            verify(productInfoRepository).findById(productId);
            verifyNoInteractions(productInfoDtoConverter, productPictureLinkUpdater);
            verifyNoMoreInteractions(productInfoRepository);
        }
    }
}

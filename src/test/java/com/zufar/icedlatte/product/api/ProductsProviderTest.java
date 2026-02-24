package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductsProvider unit tests")
class ProductsProviderTest {

    @Mock
    private ProductInfoRepository productInfoRepository;
    @Mock
    private ProductInfoDtoConverter productInfoDtoConverter;
    @Mock
    private ProductPictureLinkUpdater productPictureLinkUpdater;
    @InjectMocks
    private ProductsProvider provider;

    @Test
    @DisplayName("Returns empty list for null input without hitting repository")
    void getProducts_nullInput_returnsEmpty() {
        List<ProductInfoDto> result = provider.getProducts(null);
        assertThat(result).isEmpty();
        verifyNoInteractions(productInfoRepository);
    }

    @Test
    @DisplayName("Returns empty list for empty input without hitting repository")
    void getProducts_emptyInput_returnsEmpty() {
        List<ProductInfoDto> result = provider.getProducts(List.of());
        assertThat(result).isEmpty();
        verifyNoInteractions(productInfoRepository);
    }

    @Test
    @DisplayName("Returns products in requested order")
    void getProducts_allFound_returnsInOrder() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ProductInfo p1 = new ProductInfo();
        p1.setId(id1);
        ProductInfo p2 = new ProductInfo();
        p2.setId(id2);
        ProductInfoDto dto1 = new ProductInfoDto();
        dto1.setId(id1);
        ProductInfoDto dto2 = new ProductInfoDto();
        dto2.setId(id2);

        when(productInfoRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(p1, p2));
        when(productInfoDtoConverter.toDto(p1)).thenReturn(dto1);
        when(productInfoDtoConverter.toDto(p2)).thenReturn(dto2);
        when(productPictureLinkUpdater.updateBatch(List.of(dto1, dto2))).thenReturn(List.of(dto1, dto2));

        List<ProductInfoDto> result = provider.getProducts(List.of(id1, id2));

        assertThat(result).containsExactly(dto1, dto2);
    }

    @Test
    @DisplayName("Throws ProductNotFoundException when a requested product is missing")
    void getProducts_missingProduct_throwsProductNotFoundException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ProductInfo p1 = new ProductInfo();
        p1.setId(id1);
        ProductInfoDto dto1 = new ProductInfoDto();
        dto1.setId(id1);

        when(productInfoRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(p1));
        when(productInfoDtoConverter.toDto(p1)).thenReturn(dto1);
        when(productPictureLinkUpdater.updateBatch(List.of(dto1))).thenReturn(List.of(dto1));

        assertThatThrownBy(() -> provider.getProducts(List.of(id1, id2)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(id2.toString());
    }
}

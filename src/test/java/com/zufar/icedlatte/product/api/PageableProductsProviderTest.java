package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PageableProductsProviderTest {

    @Mock private ProductInfoRepository productRepository;
    @Mock private ProductInfoDtoConverter productInfoConverter;
    @Mock private ProductPictureLinkUpdater productPictureLinkUpdater;
    @InjectMocks
    private PageableProductsProvider productsProvider;

    @Test
    void shouldFetchProductsUsingPageAttributes() {
        Page<ProductInfo> page = new PageImpl<>(List.of(mock(ProductInfo.class)));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productInfoConverter.toDto(any(ProductInfo.class))).thenReturn(mock(ProductInfoDto.class));
        when(productPictureLinkUpdater.update(any(ProductInfoDto.class))).thenReturn(mock(ProductInfoDto.class));
        when(productInfoConverter.toProductPaginationDto(any())).thenReturn(mock(ProductListWithPaginationInfoDto.class));

        ProductListWithPaginationInfoDto result = productsProvider.getProducts(
                1, 10, "name", "ASC", null, null, null, null, null);

        assertNotNull(result);
        verify(productInfoConverter, times(1)).toProductPaginationDto(any());
    }
}

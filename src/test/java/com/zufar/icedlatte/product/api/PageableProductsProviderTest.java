package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageableProductsProviderTest {

    @Mock
    private ProductInfoRepository productRepository;
    @Mock
    private ProductInfoDtoConverter productInfoConverter;
    @Mock
    private ProductUpdater productUpdater;

    @InjectMocks
    private PageableProductsProvider productsProvider;

    @Test
    void shouldFetchProductsUsingPageAttributes() {
        final int pageNumber = 1;
        final int pageSize = 10;
        final String sortAttribute = "name";
        final String sortDirection = "ASC";

        Page<ProductInfo> page = new PageImpl<>(List.of(mock(ProductInfo.class)));
        when(productRepository.findAllProducts(null, null, null, null, null,
                PageRequest.of(pageNumber, pageSize, Sort.Direction.ASC, sortAttribute))).thenReturn(page);
        when(productInfoConverter.toDto(any(ProductInfo.class))).thenReturn(mock(ProductInfoDto.class));
        when(productUpdater.update(any(ProductInfoDto.class))).thenReturn(mock(ProductInfoDto.class));
        when(productInfoConverter.toProductPaginationDto(any())).thenReturn(mock(ProductListWithPaginationInfoDto.class));

        ProductListWithPaginationInfoDto result = productsProvider.getProducts(
                pageNumber, pageSize, sortAttribute, sortDirection, null, null, null, null, null);

        assertNotNull(result);
        verify(productInfoConverter, times(1)).toProductPaginationDto(any());
    }
}

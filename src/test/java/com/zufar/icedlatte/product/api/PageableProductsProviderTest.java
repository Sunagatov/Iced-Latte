package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
@DisplayName("PageableProductsProvider unit tests")
class PageableProductsProviderTest {

    @Mock
    private ProductInfoRepository productRepository;

    @Mock
    private ProductInfoDtoConverter productInfoConverter;

    @Mock
    private ProductPictureLinkUpdater productPictureLinkUpdater;

    @Mock
    private PaginationConfig paginationConfig;

    @InjectMocks
    private PageableProductsProvider productsProvider;

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        @DisplayName("uses explicit page attributes and returns converted pagination dto")
        void usesExplicitPageAttributesAndReturnsConvertedPaginationDto() {
            ProductInfo product = new ProductInfo();
            ProductInfoDto dto = new ProductInfoDto();
            ProductInfoDto updatedDto = new ProductInfoDto();
            ProductListWithPaginationInfoDto paginationDto = new ProductListWithPaginationInfoDto();
            Page<ProductInfo> page = new PageImpl<>(List.of(product));
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(productInfoConverter.toDto(product)).thenReturn(dto);
            when(productPictureLinkUpdater.updateBatch(List.of(dto))).thenReturn(List.of(updatedDto));
            when(productInfoConverter.toProductPaginationDto(any())).thenReturn(paginationDto);

            ProductListWithPaginationInfoDto result = productsProvider.getProducts(
                    1, 10, "price", "asc", null, null, null, null, null, "latte");

            assertThat(result).isSameAs(paginationDto);
            verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
            verify(productInfoConverter).toDto(product);
            verify(productPictureLinkUpdater).updateBatch(List.of(dto));
            verify(productInfoConverter).toProductPaginationDto(any());
            verifyNoMoreInteractions(productRepository, productInfoConverter, productPictureLinkUpdater, paginationConfig);

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort().toString()).contains("price: ASC").contains("id: ASC");
        }

        @Test
        @DisplayName("uses pagination defaults when request values are null")
        void usesPaginationDefaultsWhenRequestValuesAreNull() {
            PaginationConfig.Products products = new PaginationConfig.Products();
            products.setDefaultPageSize(50);
            products.setDefaultSortAttribute("name");
            products.setDefaultSortDirection("desc");
            ProductInfo product = new ProductInfo();
            ProductInfoDto dto = new ProductInfoDto();
            ProductListWithPaginationInfoDto paginationDto = new ProductListWithPaginationInfoDto();
            Page<ProductInfo> page = new PageImpl<>(List.of(product));
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            when(paginationConfig.getDefaultPageNumber()).thenReturn(0);
            when(paginationConfig.getProducts()).thenReturn(products);
            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(productInfoConverter.toDto(product)).thenReturn(dto);
            when(productPictureLinkUpdater.updateBatch(List.of(dto))).thenReturn(List.of(dto));
            when(productInfoConverter.toProductPaginationDto(any())).thenReturn(paginationDto);

            productsProvider.getProducts(null, null, null, null, null, null, null, null, null, null);

            verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(50);
            assertThat(pageable.getSort().toString()).contains("name: DESC").contains("id: ASC");
        }
    }
}

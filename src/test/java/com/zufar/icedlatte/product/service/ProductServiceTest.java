package com.zufar.icedlatte.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
@DisplayName("ProductService unit tests")
class ProductServiceTest {

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private ProductInfoDtoConverter productInfoDtoConverter;

    @Mock
    private ProductPictureLinkUpdater productPictureLinkUpdater;

    @Mock
    private PaginationConfig paginationConfig;

    @InjectMocks
    private ProductService productService;

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

            ProductInfoDto result = productService.getProductDtoById(productId);

            assertThat(result).isSameAs(enriched);
            verify(productInfoRepository).findById(productId);
            verify(productInfoDtoConverter).toDto(product);
            verify(productPictureLinkUpdater).update(converted);
        }

        @Test
        @DisplayName("throws ProductNotFoundException when the product does not exist")
        void throwsWhenNotFound() {
            UUID productId = UUID.randomUUID();
            when(productInfoRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductDtoById(productId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining(productId.toString());
        }
    }

    @Nested
    @DisplayName("getProductsByIds")
    class GetProductsByIds {

        @Test
        @DisplayName("returns empty list for empty input")
        void emptyInput_returnsEmpty() {
            assertThat(productService.getProductDtosByIds(List.of())).isEmpty();
            verifyNoInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("returns products in requested order")
        void allFound_returnsInOrder() {
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

            assertThat(productService.getProductDtosByIds(List.of(id1, id2))).containsExactly(dto1, dto2);
        }

        @Test
        @DisplayName("throws when a requested product is missing")
        void missingProduct_throws() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            ProductInfo p1 = new ProductInfo();
            p1.setId(id1);
            ProductInfoDto dto1 = new ProductInfoDto();
            dto1.setId(id1);

            when(productInfoRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(p1));
            when(productInfoDtoConverter.toDto(p1)).thenReturn(dto1);
            when(productPictureLinkUpdater.updateBatch(List.of(dto1))).thenReturn(List.of(dto1));

            assertThatThrownBy(() -> productService.getProductDtosByIds(List.of(id1, id2)))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining(id2.toString());
        }

        @Test
        @DisplayName("rejects duplicate requested product ids")
        void duplicateProductIds_throwsBadRequest() {
            UUID id = UUID.randomUUID();

            assertThatThrownBy(() -> productService.getProductDtosByIds(List.of(id, id)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("duplicate");

            verifyNoInteractions(productInfoRepository, productInfoDtoConverter, productPictureLinkUpdater);
        }

        @Test
        @DisplayName("rejects a null product id list")
        void nullProductIdList_throwsBadRequest() {
            assertThatThrownBy(() -> productService.getProductDtosByIds(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("null");

            verifyNoInteractions(productInfoRepository, productInfoDtoConverter, productPictureLinkUpdater);
        }

        @Test
        @DisplayName("rejects null requested product ids")
        void nullProductIds_throwsBadRequest() {
            UUID id = UUID.randomUUID();

            assertThatThrownBy(() -> productService.getProductDtosByIds(Arrays.asList(id, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("null");

            verifyNoInteractions(productInfoRepository, productInfoDtoConverter, productPictureLinkUpdater);
        }
    }

    @Nested
    @DisplayName("getProducts (paginated)")
    class GetProducts {

        @Test
        @DisplayName("uses explicit page attributes and returns converted pagination dto")
        void usesExplicitParams() {
            ProductInfo product = new ProductInfo();
            ProductInfoDto dto = new ProductInfoDto();
            ProductInfoDto updatedDto = new ProductInfoDto();
            ProductListWithPaginationInfoDto paginationDto = new ProductListWithPaginationInfoDto();
            Page<ProductInfo> page = new PageImpl<>(List.of(product));
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            when(productInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);
            when(productInfoDtoConverter.toDto(product)).thenReturn(dto);
            when(productPictureLinkUpdater.updateBatch(List.of(dto))).thenReturn(List.of(updatedDto));
            when(productInfoDtoConverter.toProductPaginationDto(any())).thenReturn(paginationDto);

            ProductListWithPaginationInfoDto result =
                    productService.getProductDtos(1, 10, "price", "asc", null, null, null, null, null, "latte");

            assertThat(result).isSameAs(paginationDto);
            verify(productInfoRepository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("uses pagination defaults when request values are null")
        void usesDefaults() {
            ProductInfo product = new ProductInfo();
            ProductInfoDto dto = new ProductInfoDto();
            ProductListWithPaginationInfoDto paginationDto = new ProductListWithPaginationInfoDto();
            Page<ProductInfo> page = new PageImpl<>(List.of(product));
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            var products = new PaginationConfig.Products(50, "name", "desc");
            when(paginationConfig.defaultPageNumber()).thenReturn(0);
            when(paginationConfig.products()).thenReturn(products);
            when(productInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);
            when(productInfoDtoConverter.toDto(product)).thenReturn(dto);
            when(productPictureLinkUpdater.updateBatch(List.of(dto))).thenReturn(List.of(dto));
            when(productInfoDtoConverter.toProductPaginationDto(any())).thenReturn(paginationDto);

            productService.getProductDtos(null, null, null, null, null, null, null, null, null, null);

            verify(productInfoRepository).findAll(any(Specification.class), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("filter options")
    class FilterOptions {

        @Test
        @DisplayName("getSellerNames delegates to repository")
        void sellerNames() {
            when(productInfoRepository.findDistinctSellerNames()).thenReturn(List.of("A", "B"));
            assertThat(productService.getSellerNames()).containsExactly("A", "B");
            verify(productInfoRepository).findDistinctSellerNames();
            verifyNoMoreInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("getBrandNames delegates to repository")
        void brandNames() {
            when(productInfoRepository.findDistinctBrandNames()).thenReturn(List.of("X", "Y"));
            assertThat(productService.getBrandNames()).containsExactly("X", "Y");
            verify(productInfoRepository).findDistinctBrandNames();
            verifyNoMoreInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("existsById delegates to repository")
        void existsByIdDelegatesToRepository() {
            UUID productId = UUID.randomUUID();
            when(productInfoRepository.existsById(productId)).thenReturn(true);

            assertThat(productService.existsById(productId)).isTrue();

            verify(productInfoRepository).existsById(productId);
            verifyNoMoreInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("findExistingProductIds returns empty set for empty input")
        void findExistingProductIdsReturnsEmptyForEmptyInput() {
            assertThat(productService.findExistingProductIds(Set.of())).isEmpty();

            verifyNoInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("findExistingProductIds rejects null product id set")
        void findExistingProductIdsRejectsNullSet() {
            assertThatThrownBy(() -> productService.findExistingProductIds(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("null");

            verifyNoInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("findExistingProductIds rejects null product id values")
        void findExistingProductIdsRejectsNullValues() {
            Set<UUID> productIds = new HashSet<>();
            productIds.add(null);

            assertThatThrownBy(() -> productService.findExistingProductIds(productIds))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("null");

            verifyNoInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("findExistingProductIds delegates with validated ids")
        void findExistingProductIdsDelegatesWithValidatedIds() {
            UUID productId = UUID.randomUUID();
            when(productInfoRepository.findExistingIds(Set.of(productId))).thenReturn(Set.of(productId));

            assertThat(productService.findExistingProductIds(Set.of(productId))).containsExactly(productId);

            verify(productInfoRepository).findExistingIds(Set.of(productId));
            verifyNoMoreInteractions(productInfoRepository);
        }
    }
}

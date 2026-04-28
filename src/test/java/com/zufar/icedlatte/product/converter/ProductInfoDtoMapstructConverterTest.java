package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductInfoDtoConverter unit tests")
class ProductInfoDtoMapstructConverterTest {

    private final ProductInfoDtoConverter converter = Mappers.getMapper(ProductInfoDtoConverter.class);

    @Nested
    @DisplayName("toDto")
    class ToDto {

        @Test
        @DisplayName("maps product fields and applies converter-specific transformations")
        void mapsProductFieldsAndAppliesConverterSpecificTransformations() {
            UUID productId = UUID.randomUUID();
            LocalDateTime dateAdded = LocalDateTime.of(2026, 4, 28, 12, 15, 30);
            ProductInfo product = new ProductInfo();
            product.setId(productId);
            product.setName("Product A");
            product.setDescription("Description");
            product.setPrice(BigDecimal.valueOf(10.50));
            product.setQuantity(20);
            product.setActive(true);
            product.setBrandName("Brand A");
            product.setSellerName("Seller A");
            product.setAverageRating(new BigDecimal("4.44"));
            product.setDateAdded(dateAdded);

            ProductInfoDto result = converter.toDto(product);

            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo("Product A");
            assertThat(result.getDescription()).isEqualTo("Description");
            assertThat(result.getPrice()).isEqualByComparingTo("10.50");
            assertThat(result.getQuantity()).isEqualTo(20);
            assertThat(result.getActive()).isTrue();
            assertThat(result.getBrandName()).isEqualTo("Brand A");
            assertThat(result.getSellerName()).isEqualTo("Seller A");
            assertThat(result.getAverageRating()).isEqualByComparingTo("4.4");
            assertThat(result.getDateAdded()).isEqualTo(OffsetDateTime.of(dateAdded, ZoneOffset.UTC));
            assertThat(result.getProductFileUrl()).isNull();
        }

        @Test
        @DisplayName("keeps nullable converted fields null")
        void keepsNullableConvertedFieldsNull() {
            ProductInfo product = new ProductInfo();
            product.setId(UUID.randomUUID());

            ProductInfoDto result = converter.toDto(product);

            assertThat(result.getAverageRating()).isNull();
            assertThat(result.getDateAdded()).isNull();
        }
    }

    @Nested
    @DisplayName("toProductPaginationDto")
    class ToProductPaginationDto {

        @Test
        @DisplayName("maps page metadata and preserves product order")
        void mapsPageMetadataAndPreservesProductOrder() {
            ProductInfoDto first = new ProductInfoDto();
            first.setId(UUID.randomUUID());
            first.setName("First");
            ProductInfoDto second = new ProductInfoDto();
            second.setId(UUID.randomUUID());
            second.setName("Second");
            Page<ProductInfoDto> page = new PageImpl<>(List.of(first, second), PageRequest.of(2, 2), 5);

            ProductListWithPaginationInfoDto result = converter.toProductPaginationDto(page);

            assertThat(result.getProducts()).containsExactly(first, second);
            assertThat(result.getPage()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(page.getTotalElements());
            assertThat(result.getTotalPages()).isEqualTo(page.getTotalPages());
        }
    }
}

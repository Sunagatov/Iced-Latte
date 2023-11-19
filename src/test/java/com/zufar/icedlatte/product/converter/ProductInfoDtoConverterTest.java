package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.util.ProductStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductInfoDtoConverterTest {

    private ProductInfoDtoConverter converter;

    @BeforeEach
    void setup() {
        converter = Mappers.getMapper(ProductInfoDtoConverter.class);
    }

    @Test
    @DisplayName("Convert ProductInfo entity to ProductInfoDto")
    void toDto_ShouldMapProductInfoToProductInfoDto() {
        ProductInfo expectedProductInfoDto = ProductStub.generateSampleEntityProduct();

        ProductInfoDto actualProductInfoDto = converter.toDto(expectedProductInfoDto);

        assertThat(actualProductInfoDto.getId()).isEqualTo(expectedProductInfoDto.getProductId());
        assertThat(actualProductInfoDto.getName()).isEqualTo(expectedProductInfoDto.getName());
        assertThat(actualProductInfoDto.getDescription()).isEqualTo(expectedProductInfoDto.getDescription());
        assertThat(actualProductInfoDto.getPrice()).isEqualTo(expectedProductInfoDto.getPrice());
        assertThat(actualProductInfoDto.getQuantity()).isEqualTo(expectedProductInfoDto.getQuantity());
        assertThat(actualProductInfoDto.getActive()).isEqualTo(expectedProductInfoDto.getActive());

    }

    @Test
    @DisplayName("Convert Page of ProductInfoDto to ProductListWithPaginationInfoDto")
    void toProductPaginationDto_ShouldMapPageProductInfoDtoToProductListWithPaginationInfoDto() {
        List<ProductInfoDto> productInfoDtos = ProductStub.generateSampleProducts();
        Page<ProductInfoDto> page = new PageImpl<>(productInfoDtos, PageRequest.of(0, 5), productInfoDtos.size());

        ProductListWithPaginationInfoDto paginationInfoDto = converter.toProductPaginationDto(page);

        assertThat(paginationInfoDto.getProducts()).hasSize(productInfoDtos.size());
        assertThat(paginationInfoDto.getPage()).isEqualTo(page.getNumber());
        assertThat(paginationInfoDto.getSize()).isEqualTo(page.getSize());
        assertThat(paginationInfoDto.getTotalElements()).isEqualTo(productInfoDtos.size());
        assertThat(paginationInfoDto.getTotalPages()).isEqualTo(1);
    }
}

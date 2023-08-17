package com.zufar.onlinestore.product;

import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductInfoFullDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ProductInfoDtoConverterTest {

    @Autowired
    private ProductInfoDtoConverter converter;
    private ProductInfo productInfo;
    private Page<ProductInfoDto> productInfoDtoPage;

    @BeforeEach
    void setUp() {
        productInfo = Instancio.create(ProductInfo.class);

        List<ProductInfoDto> infoDtoList = Instancio.createList(ProductInfoDto.class);
        productInfoDtoPage = new PageImpl<>(infoDtoList);
    }

    @Test
    void shouldCheckProductInfoDoesNotEqualToNull() {
        assertNotNull(productInfo);
    }

    @Test
    void shouldCheckProductInfoDtoDoesNotEqualToNull() {
        ProductInfoDto dto = converter.toDto(productInfo);

        assertNotNull(dto);
    }

    @Test
    void shouldMapProductInfoToProductInfoDto() {
        ProductInfoDto dto = converter.toDto(productInfo);

        assertAll(
                () -> assertEquals(dto.id(), productInfo.getProductId()),
                () -> assertEquals(dto.name(), productInfo.getName()),
                () -> assertEquals(dto.description(), productInfo.getDescription()),
                () -> assertEquals(dto.quantity(), productInfo.getQuantity())
        );
    }

    @Test
    void shouldCheckProductInfoFullDtoDoesNotEqualToNull() {
        ProductInfoFullDto dto = converter.toFullDto(productInfo);

        assertNotNull(dto);
    }

    @Test
    void shouldMapProductInfoToProductInfoFullDto() {
        ProductInfoFullDto dto = converter.toFullDto(productInfo);

        assertAll(
                () -> assertEquals(dto.id(), productInfo.getProductId()),
                () -> assertEquals(dto.name(), productInfo.getName()),
                () -> assertEquals(dto.description(), productInfo.getDescription()),
                () -> assertEquals(dto.quantity(), productInfo.getQuantity()),
                () -> assertEquals(dto.active(), productInfo.getActive())
        );
    }

    @Test
    void shouldCheckPageOfProductInfoDtoDoesNotEqualToNull() {
        ProductListWithPaginationInfoDto paginationDto = converter.toProductPaginationDto(productInfoDtoPage);

        assertNotNull(paginationDto);
    }

    @Test
    void shouldMapPageOfProductInfoDtoToProductListWithPaginationInfoDto() {
        int pageSize = productInfoDtoPage.getSize();
        int totalPages = productInfoDtoPage.getTotalPages();
        long totalElements = productInfoDtoPage.getTotalElements();
        int productsSize = productInfoDtoPage.getContent().size();
        Optional<ProductInfoDto> firstProduct = productInfoDtoPage.getContent().stream().findFirst();

        ProductListWithPaginationInfoDto paginationDto = converter.toProductPaginationDto(productInfoDtoPage);

        assertAll(
                () -> assertEquals(paginationDto.size(), pageSize),
                () -> assertEquals(paginationDto.totalPages(), totalPages),
                () -> assertEquals(paginationDto.totalElements(), totalElements),
                () -> assertEquals(paginationDto.products().size(), productsSize),
                () -> assertEquals(paginationDto.products().stream().findFirst(), firstProduct));
    }
}
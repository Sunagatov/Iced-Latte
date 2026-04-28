package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FavoriteItemDtoConverter")
class FavoriteItemDtoConverterTest {

    private final FavoriteItemDtoConverter converter;

    FavoriteItemDtoConverterTest() {
        FavoriteItemDtoConverterImpl mapper = new FavoriteItemDtoConverterImpl();
        ReflectionTestUtils.setField(
                mapper,
                "productInfoDtoConverter",
                Mappers.getMapper(ProductInfoDtoConverter.class)
        );
        this.converter = mapper;
    }

    @Test
    @DisplayName("maps favorite item and converts product date to UTC offset")
    void toDto_mapsFavoriteItemAndProductInfo() {
        UUID itemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime dateAdded = LocalDateTime.of(2026, 4, 29, 9, 15, 30);

        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(productId);
        productInfo.setName("Cold Brew");
        productInfo.setDescription("Smooth and strong");
        productInfo.setPrice(new BigDecimal("12.50"));
        productInfo.setQuantity(7);
        productInfo.setActive(true);
        productInfo.setAverageRating(new BigDecimal("4.7"));
        productInfo.setReviewsCount(18);
        productInfo.setBrandName("Iced Latte");
        productInfo.setSellerName("Warehouse");
        productInfo.setOriginCountry("United Kingdom");
        productInfo.setWeight(250);
        productInfo.setLength(10);
        productInfo.setWidth(5);
        productInfo.setHeight(18);
        productInfo.setSoldProductsCount(120);
        productInfo.setDiscount(15);
        productInfo.setDateAdded(dateAdded);
        productInfo.setPopularityScore(88);

        FavoriteItemEntity entity = new FavoriteItemEntity();
        entity.setId(itemId);
        entity.setProductInfo(productInfo);

        FavoriteItemDto dto = converter.toDto(entity);

        assertThat(dto.id()).isEqualTo(itemId);
        assertThat(dto.productInfo().getId()).isEqualTo(productId);
        assertThat(dto.productInfo().getName()).isEqualTo("Cold Brew");
        assertThat(dto.productInfo().getPrice()).isEqualByComparingTo("12.50");
        assertThat(dto.productInfo().getDateAdded())
                .isEqualTo(OffsetDateTime.of(dateAdded, ZoneOffset.UTC));
        assertThat(dto.productInfo().getPopularityScore()).isEqualTo(88);
    }

    @Test
    @DisplayName("keeps null product date when source date is absent")
    void toDto_keepsNullDateAddedWhenSourceDateIsNull() {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(UUID.randomUUID());
        productInfo.setName("Cold Brew");
        productInfo.setDescription("Smooth and strong");
        productInfo.setPrice(BigDecimal.ONE);
        productInfo.setQuantity(1);
        productInfo.setActive(true);
        productInfo.setAverageRating(BigDecimal.ZERO);
        productInfo.setReviewsCount(0);
        productInfo.setBrandName("Iced Latte");
        productInfo.setSellerName("Warehouse");
        productInfo.setOriginCountry("United Kingdom");
        productInfo.setWeight(1);
        productInfo.setLength(1);
        productInfo.setWidth(1);
        productInfo.setHeight(1);
        productInfo.setSoldProductsCount(0);
        productInfo.setDiscount(0);
        productInfo.setPopularityScore(0);

        FavoriteItemEntity entity = new FavoriteItemEntity();
        entity.setId(UUID.randomUUID());
        entity.setProductInfo(productInfo);

        FavoriteItemDto dto = converter.toDto(entity);

        assertThat(dto.productInfo().getDateAdded()).isNull();
    }
}

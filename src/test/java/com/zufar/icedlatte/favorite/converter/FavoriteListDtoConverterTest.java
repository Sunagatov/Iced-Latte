package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FavoriteListDtoConverterTest {

    private FavoriteListDtoConverter converter;

    @BeforeEach
    void setup() {
        converter = Mappers.getMapper(FavoriteListDtoConverter.class);
    }

    @Test
    @DisplayName("Convert FavoriteListEntity to FavoriteListDto")
    void convertListEntityToDto() {

        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(UUID.randomUUID());

        FavoriteItemEntity favoriteItem = new FavoriteItemEntity();
        favoriteItem.setProductInfo(productInfo);

        FavoriteListEntity expectedFavoriteListEntity = FavoriteListEntity.builder()
                .id(UUID.randomUUID())
                .favoriteItems(Set.of(favoriteItem))
                .updatedAt(OffsetDateTime.now())
                .build();

        FavoriteListDto actualFavoriteListDto = converter.toDto(expectedFavoriteListEntity);

        assertEquals(actualFavoriteListDto.id(), expectedFavoriteListEntity.getId());
        assertEquals(actualFavoriteListDto.updatedAt(), expectedFavoriteListEntity.getUpdatedAt());
    }

    @Test
    @DisplayName("Convert ProductInfo to ProductInfoDto")
    void convertToProductInfoDto() {

        ProductInfo expectedProductInfo = new ProductInfo(UUID.randomUUID(), 1L, "Coffee", "Coffee description",
                new BigDecimal(100), 1, true,  new BigDecimal(100), 1, "Jacobs", "Seller",
                "originCountry", 100, 10, 4, 25, 200, 20, LocalDateTime.now(), 60, null);

        ProductInfoDto actualProductInfoDto = converter.convertProductInfoDto(expectedProductInfo);

        assertThat(actualProductInfoDto.getId()).isEqualTo(expectedProductInfo.getId());
        assertThat(actualProductInfoDto.getName()).isEqualTo(expectedProductInfo.getName());
        assertThat(actualProductInfoDto.getDescription()).isEqualTo(expectedProductInfo.getDescription());
        assertThat(actualProductInfoDto.getPrice()).isEqualTo(expectedProductInfo.getPrice());
        assertThat(actualProductInfoDto.getQuantity()).isEqualTo(expectedProductInfo.getQuantity());
        assertThat(actualProductInfoDto.getActive()).isEqualTo(expectedProductInfo.isActive());
        assertThat(actualProductInfoDto.getAverageRating()).isEqualTo(expectedProductInfo.getAverageRating());
        assertThat(actualProductInfoDto.getReviewsCount()).isEqualTo(expectedProductInfo.getReviewsCount());
        assertThat(actualProductInfoDto.getBrandName()).isEqualTo(expectedProductInfo.getBrandName());
        assertThat(actualProductInfoDto.getSellerName()).isEqualTo(expectedProductInfo.getSellerName());
    }

}

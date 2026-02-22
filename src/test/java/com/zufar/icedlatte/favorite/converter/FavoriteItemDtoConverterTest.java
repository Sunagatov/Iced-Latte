package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class FavoriteItemDtoConverterTest {

    private FavoriteItemDtoConverter converter;

    @BeforeEach
    void setup() {
        converter = Mappers.getMapper(FavoriteItemDtoConverter.class);
        ReflectionTestUtils.setField(converter, "productInfoDtoConverter", Mappers.getMapper(ProductInfoDtoConverter.class));
    }

    @Test
    @DisplayName("Convert FavoriteItemEntity to FavoriteItemDto")
    void toDtoTest() {

        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(UUID.randomUUID());

        FavoriteItemEntity expectedFavoriteItem = new FavoriteItemEntity();
        expectedFavoriteItem.setProductInfo(productInfo);

        converter.toDto(expectedFavoriteItem);
        // FIXME: different types of sets
        //assertThat(actualFavoriteItemDto.productInfo()).isEqualTo(expectedFavoriteItem.getProductInfo());
    }
}

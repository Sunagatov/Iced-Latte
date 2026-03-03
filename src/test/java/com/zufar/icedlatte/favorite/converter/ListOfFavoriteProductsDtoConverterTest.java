package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListOfFavoriteProductsDtoConverterTest {

    private ListOfFavoriteProductsDtoConverter converter;

    @BeforeEach
    void setup() {
        converter = Mappers.getMapper(ListOfFavoriteProductsDtoConverter.class);
    }

    @Test
    @DisplayName("Convert FavoriteListDto to ListOfFavoriteProductsDto")
    void toListProductDto() {

        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(UUID.randomUUID());

        FavoriteItemDto favoriteItemDto = new FavoriteItemDto(
                UUID.randomUUID(),
                productInfoDto);

        FavoriteListDto expectedFavoriteListDto = new FavoriteListDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Set.of(favoriteItemDto),
                OffsetDateTime.now());

        ListOfFavoriteProductsDto actualListOfFavoriteProductsDto = converter.toListProductDto(expectedFavoriteListDto);

        assertThat(actualListOfFavoriteProductsDto.getProducts()).isEqualTo(converter.toProductInfoDto(expectedFavoriteListDto.favoriteItems()));

        assertEquals(actualListOfFavoriteProductsDto.getProducts().getFirst(), productInfoDto);
    }
}

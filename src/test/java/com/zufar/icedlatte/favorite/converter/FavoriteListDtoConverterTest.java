package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FavoriteListDtoConverterTest {

    private final FavoriteListDtoConverter converter = new FavoriteListDtoConverter();

    @Test
    @DisplayName("Convert FavoriteListEntity to FavoriteListDto with product map")
    void convertListEntityToDto() {
        UUID productId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        FavoriteItemEntity favoriteItem = FavoriteItemEntity.builder()
                .id(itemId)
                .productId(productId)
                .build();

        FavoriteListEntity entity = FavoriteListEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .favoriteItems(Set.of(favoriteItem))
                .updatedAt(OffsetDateTime.now())
                .build();

        ProductInfoDto productDto = new ProductInfoDto();
        productDto.setId(productId);
        productDto.setName("Coffee");
        productDto.setPrice(BigDecimal.valueOf(10));

        Map<UUID, ProductInfoDto> productsById = Map.of(productId, productDto);

        FavoriteListDto result = converter.toDto(entity, productsById);

        assertThat(result.id()).isEqualTo(entity.getId());
        assertThat(result.userId()).isEqualTo(entity.getUserId());
        assertThat(result.updatedAt()).isEqualTo(entity.getUpdatedAt());
        assertThat(result.favoriteItems()).hasSize(1);

        FavoriteItemDto itemDto = result.favoriteItems().iterator().next();
        assertThat(itemDto.id()).isEqualTo(itemId);
        assertThat(itemDto.productInfo().getId()).isEqualTo(productId);
        assertThat(itemDto.productInfo().getName()).isEqualTo("Coffee");
    }

    @Test
    @DisplayName("Filters out items whose product is not in the map")
    void filtersOutMissingProducts() {
        UUID knownProductId = UUID.randomUUID();
        UUID unknownProductId = UUID.randomUUID();

        FavoriteListEntity entity = FavoriteListEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .favoriteItems(Set.of(
                        FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(knownProductId).build(),
                        FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(unknownProductId).build()
                ))
                .updatedAt(OffsetDateTime.now())
                .build();

        ProductInfoDto productDto = new ProductInfoDto();
        productDto.setId(knownProductId);
        Map<UUID, ProductInfoDto> productsById = Map.of(knownProductId, productDto);

        FavoriteListDto result = converter.toDto(entity, productsById);

        assertThat(result.favoriteItems()).hasSize(1);
        assertThat(result.favoriteItems().iterator().next().productInfo().getId()).isEqualTo(knownProductId);
    }
}

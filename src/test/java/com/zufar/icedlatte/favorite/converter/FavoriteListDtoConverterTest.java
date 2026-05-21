package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FavoriteListDtoConverterTest {

    private final FavoriteListDtoConverter converter = new FavoriteListDtoConverter();

    @Test
    @DisplayName("converts entity to ListOfFavoriteProductsDto with product details")
    void convertEntityToDto() {
        UUID productId = UUID.randomUUID();

        FavoriteListEntity entity = FavoriteListEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .favoriteItems(Set.of(
                        FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(productId).build()
                ))
                .updatedAt(OffsetDateTime.now())
                .build();

        ListOfFavoriteProductsDto result = converter.toDto(entity, Map.of(productId, productSnapshot(productId)));

        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getProducts().getFirst().getId()).isEqualTo(productId);
        assertThat(result.getProducts().getFirst().getName()).isEqualTo("Coffee");
    }

    @Test
    @DisplayName("filters out items whose product is not in the map")
    void filtersOutMissingProducts() {
        UUID knownId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();

        FavoriteListEntity entity = FavoriteListEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .favoriteItems(Set.of(
                        FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(knownId).build(),
                        FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(unknownId).build()
                ))
                .updatedAt(OffsetDateTime.now())
                .build();

        ListOfFavoriteProductsDto result = converter.toDto(entity, Map.of(knownId, productSnapshot(knownId)));

        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getProducts().getFirst().getId()).isEqualTo(knownId);
    }

    private static ProductSnapshot productSnapshot(UUID id) {
        return new ProductSnapshot(id, "Coffee", null, BigDecimal.valueOf(10), null, true, null, List.of(), null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}

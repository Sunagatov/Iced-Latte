package com.zufar.icedlatte.favorite.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductSummaryDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

class FavoriteListDtoConverterTest {

    private final FavoriteListDtoConverter converter = new FavoriteListDtoConverter();

    @Test
    @DisplayName("converts product snapshots to ListOfFavoriteProductsDto")
    void convertSnapshotsToDto() {
        UUID productId = UUID.randomUUID();

        ListOfFavoriteProductsDto result = converter.toDto(List.of(productSnapshot(productId)));

        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getProducts().getFirst().getId()).isEqualTo(productId);
        assertThat(result.getProducts().getFirst().getName()).isEqualTo("Coffee");
    }

    @Test
    @DisplayName("sorts products by id")
    void sortsProductsById() {
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ListOfFavoriteProductsDto result =
                converter.toDto(List.of(productSnapshot(secondId), productSnapshot(firstId)));

        assertThat(result.getProducts()).extracting(ProductSummaryDto::getId).containsExactly(firstId, secondId);
    }

    private static ProductSnapshot productSnapshot(UUID id) {
        return new ProductSnapshot(id, "Coffee", "Desc", BigDecimal.valueOf(10), 100, true, null);
    }
}

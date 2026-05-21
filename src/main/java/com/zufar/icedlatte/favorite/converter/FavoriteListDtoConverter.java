package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FavoriteListDtoConverter {

    public FavoriteListDto toDto(final FavoriteListEntity entity, final Map<UUID, ProductSnapshot> productsById) {
        Set<FavoriteItemDto> items = entity.getFavoriteItems().stream()
                .map(item -> {
                    ProductSnapshot product = productsById.get(item.getProductId());
                    return new FavoriteItemDto(item.getId(), product == null ? null : toDto(product));
                })
                .filter(item -> item.productInfo() != null)
                .collect(Collectors.toSet());

        return new FavoriteListDto(entity.getId(), entity.getUserId(), items, entity.getUpdatedAt());
    }

    private ProductInfoDto toDto(ProductSnapshot product) {
        return new ProductInfoDto()
                .id(product.id())
                .name(product.name())
                .description(product.description())
                .price(product.price())
                .quantity(product.quantity())
                .active(product.active())
                .productFileUrl(product.productFileUrl())
                .productImageUrls(product.productImageUrls())
                .averageRating(product.averageRating())
                .reviewsCount(product.reviewsCount())
                .aiSummary(product.aiSummary())
                .brandName(product.brandName())
                .sellerName(product.sellerName())
                .originCountry(product.originCountry())
                .weight(product.weight())
                .length(product.length())
                .width(product.width())
                .height(product.height())
                .soldProductsCount(product.soldProductsCount())
                .discount(product.discount())
                .dateAdded(product.dateAdded())
                .popularityScore(product.popularityScore());
    }
}

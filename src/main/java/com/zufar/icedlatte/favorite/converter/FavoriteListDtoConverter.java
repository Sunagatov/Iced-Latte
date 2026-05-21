package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FavoriteListDtoConverter {

    public ListOfFavoriteProductsDto toDto(final FavoriteListEntity entity, final Map<UUID, ProductSnapshot> productsById) {
        List<ProductInfoDto> products = entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .filter(productsById::containsKey)
                .distinct()
                .map(productsById::get)
                .map(this::toProductInfoDto)
                .toList();

        return new ListOfFavoriteProductsDto(products);
    }

    private ProductInfoDto toProductInfoDto(ProductSnapshot product) {
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

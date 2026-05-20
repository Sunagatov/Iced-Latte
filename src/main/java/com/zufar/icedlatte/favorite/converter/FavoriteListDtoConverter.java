package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FavoriteListDtoConverter {

    public FavoriteListDto toDto(final FavoriteListEntity entity, final Map<UUID, ProductInfoDto> productsById) {
        Set<FavoriteItemDto> items = entity.getFavoriteItems().stream()
                .map(item -> new FavoriteItemDto(item.getId(), productsById.get(item.getProductId())))
                .filter(item -> item.productInfo() != null)
                .collect(Collectors.toSet());

        return new FavoriteListDto(entity.getId(), entity.getUserId(), items, entity.getUpdatedAt());
    }
}

package com.zufar.onlinestore.cart.converter;

import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class ShoppingSessionDtoConverter {

    private final ShoppingSessionItemDtoConverter shoppingSessionItemDtoConverter;

    public ShoppingSessionDto convertToDto(final ShoppingSession entity) {
        Collection<ShoppingSessionItemDto> items = entity.getItems().stream()
                .map(shoppingSessionItemDtoConverter::convertToDto)
                .toList();

        return new ShoppingSessionDto(
                entity.getId(),
                entity.getUserId(),
                items,
                entity.getItemsQuantity(),
                entity.getProductsQuantity(),
                entity.getCreatedAt(),
                entity.getClosedAt()
        );
    }

    public ShoppingSession convertToEntity(final ShoppingSessionDto dto) {
        Collection<ShoppingSessionItem> items = dto.items().stream()
                .map(shoppingSessionItemDtoConverter::convertToEntity)
                .toList();

        return new ShoppingSession(
                dto.id(),
                dto.userId(),
                items,
                dto.itemsQuantity(),
                dto.productsQuantity(),
                dto.createdAt(),
                dto.closedAt()
        );
    }
}
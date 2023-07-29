package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartApiImpl implements CartApi {

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;
    private final ShoppingSessionProvider shoppingSessionProvider;

    @Override
    public ShoppingSessionDto getShoppingSession(final UUID userId) {
        return shoppingSessionProvider.getByUserId(userId);
    }

    @Override
    public ShoppingSessionDto addNewItemToShoppingSession(final AddNewItemToShoppingSessionRequest addNewItemToShoppingSessionRequest) {
        return null;
    }

    @Override
    public ShoppingSessionDto removeItemFromShoppingSession(final RemoveItemFromShoppingSessionRequest removeItemFromShoppingSessionRequest) {
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UUID userId,
                                                                          final UUID shoppingSessionItemId,
                                                                          final Integer productsQuantityChange) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException {
        ShoppingSessionDto shoppingSession = shoppingSessionProvider.getByUserId(userId);

        Integer updatedRowsQuantity =
                shoppingSessionItemRepository.updateProductsQuantityInShoppingSessionItem(shoppingSessionItemId, productsQuantityChange);

        if (updatedRowsQuantity == null || updatedRowsQuantity == 0) {
            log.error("Failed to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                    productsQuantityChange, shoppingSessionItemId, shoppingSession.id());
            throw new ShoppingSessionItemNotFoundException(shoppingSession.id(), shoppingSessionItemId);
        }
        Optional<ShoppingSessionItem> shoppingSessionItem = shoppingSessionItemRepository.findById(shoppingSessionItemId);
        if (shoppingSessionItem.isEmpty()) {
            log.error("Failed to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                    productsQuantityChange, shoppingSessionItemId, shoppingSession.id());
            throw new ShoppingSessionItemNotFoundException(shoppingSession.id(), shoppingSessionItemId);
        }
        ShoppingSessionItem updatedItem = shoppingSessionItem.get();

        ShoppingSession updatedShoppingSession = updatedItem.getShoppingSession();

        if (shoppingSession.id() != updatedShoppingSession.getId()) {
            log.error("Failed to update the productsQuantity with the change = {} in the shoppingSession with id: {} of the shoppingSession with the id = {}.",
                    productsQuantityChange, shoppingSession.id(), shoppingSessionItemId);

            throw new InvalidShoppingSessionIdInUpdateProductsQuantityRequestException(shoppingSessionItemId);
        }

        return shoppingSessionDtoConverter.toDto(updatedShoppingSession);
    }


}

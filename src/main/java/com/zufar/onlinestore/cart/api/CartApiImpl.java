package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.api.service.ProductsQuantityItemUpdater;
import com.zufar.onlinestore.cart.api.service.ShoppingSessionItemSaver;
import com.zufar.onlinestore.cart.dto.AddNewProductToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.GetShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartApiImpl implements CartApi {

    private final ShoppingSessionItemSaver shoppingSessionItemSaver;
    private final ProductsQuantityItemUpdater productsQuantityItemUpdater;
    private static final String FAILED_TO_UPDATE_THE_PRODUCTS_QUANTITY = "Failed to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.";

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final ShoppingSessionItemsDeleter shoppingSessionItemsDeleter;

    @Override
    public ShoppingSessionDto getShoppingSessionByUserId(final UUID userId) throws ShoppingSessionNotFoundException {
        return shoppingSessionProvider.getByUserId(userId);
    }

    @Override
    public ShoppingSessionDto addNewProductToShoppingSession(final UUID userId, final UUID productId) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, ProductNotFoundException {
        return shoppingSessionItemSaver.save(userId, productId);
    }

    @Override
    public ShoppingSessionDto deleteItemsFromShoppingSession(final DeleteItemsFromShoppingSessionRequest deleteItemsFromShoppingSessionRequest) {
        return shoppingSessionItemsDeleter.delete(deleteItemsFromShoppingSessionRequest);
    }

    @Override
    public ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest request) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException {
        ShoppingSessionItem updatedItem =
                shoppingSessionItemRepository.updateProductsQuantityInShoppingSessionItem(request.shoppingSessionItemId(), request.productsQuantityChange());

        if (updatedItem == null) {
            log.warn(FAILED_TO_UPDATE_THE_PRODUCTS_QUANTITY,
                    request.productsQuantityChange(), request.shoppingSessionItemId(), request.shoppingSessionId());

            throw new ShoppingSessionItemNotFoundException(request.shoppingSessionId(), request.shoppingSessionItemId());
        }

        Optional<ShoppingSession> shoppingSession = shoppingSessionRepository.findById(request.shoppingSessionId());
        if (shoppingSession.isEmpty()) {
            log.warn(FAILED_TO_UPDATE_THE_PRODUCTS_QUANTITY,
                    request.productsQuantityChange(), request.shoppingSessionItemId(), request.shoppingSessionId());

            throw new ShoppingSessionNotFoundException(request.shoppingSessionId());
        }

        if (request.shoppingSessionId() != updatedItem.getShoppingSession().getId()) {
            log.warn(FAILED_TO_UPDATE_THE_PRODUCTS_QUANTITY,
                    request.productsQuantityChange(), request.shoppingSessionItemId(), request.shoppingSessionId());

            throw new InvalidShoppingSessionIdInUpdateProductsQuantityRequestException(request.shoppingSessionId());
        }

        return shoppingSessionDtoConverter.toDto(shoppingSession.get());
        UUID shoppingSessionId = request.shoppingSessionId();
        UUID shoppingSessionItemId = request.shoppingSessionItemId();
        Integer productsQuantityChange = request.productsQuantityChange();
        return productsQuantityItemUpdater.update(shoppingSessionId, shoppingSessionItemId, productsQuantityChange);
    }
}

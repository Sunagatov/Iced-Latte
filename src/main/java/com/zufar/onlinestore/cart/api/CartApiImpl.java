package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.api.service.ProductsQuantityItemUpdater;
import com.zufar.onlinestore.cart.api.service.AddItemToShoppingSessionHelper;
import com.zufar.onlinestore.cart.dto.DeleteItemsFromShoppingSessionRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CartApiImpl implements CartApi {

    private final AddItemToShoppingSessionHelper addItemToShoppingSessionHelper;
    private final ProductsQuantityItemUpdater productsQuantityItemUpdater;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final ShoppingSessionItemsDeleter shoppingSessionItemsDeleter;

    @Override
    public ShoppingSessionDto getShoppingSessionByUserId(final UUID userId) throws ShoppingSessionNotFoundException {
        return shoppingSessionProvider.getByUserId(userId);
    }

    @Override
    public ShoppingSessionDto addNewProductToShoppingSession(final UUID userId, final UUID productId) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, ProductNotFoundException {
        return addItemToShoppingSessionHelper.add(userId, productId);
    }

    @Override
    public ShoppingSessionDto deleteItemsFromShoppingSession(final DeleteItemsFromShoppingSessionRequest deleteItemsFromShoppingSessionRequest) {
        return shoppingSessionItemsDeleter.delete(deleteItemsFromShoppingSessionRequest);
    }

    @Override
    public ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest request) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException {
        UUID shoppingSessionId = request.shoppingSessionId();
        UUID shoppingSessionItemId = request.shoppingSessionItemId();
        Integer productsQuantityChange = request.productsQuantityChange();
        return productsQuantityItemUpdater.update(shoppingSessionId, shoppingSessionItemId, productsQuantityChange);
    }
}

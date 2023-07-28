package com.zufar.onlinestore.cart.api;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class CartApiImpl implements CartApi {

    private final ShoppingSessionItemSaver shoppingSessionItemSaver;
    private final ProductsQuantityItemUpdater productsQuantityItemUpdater;

    @Override
    public ShoppingSessionDto getShoppingSession(final GetShoppingSessionRequest getShoppingSessionRequest) {
        return null;
    }

    @Override
    public ShoppingSessionDto addNewProductToShoppingSession(final UUID userId, final UUID productId) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, ProductNotFoundException {
        return shoppingSessionItemSaver.save(userId, productId);
    }

    @Override
    public ShoppingSessionDto removeItemFromShoppingSession(final RemoveItemFromShoppingSessionRequest removeItemFromShoppingSessionRequest) {
        return null;
    }

    @Override
    public ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest request) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException {
        UUID shoppingSessionId = request.shoppingSessionId();
        UUID shoppingSessionItemId = request.shoppingSessionItemId();
        Integer productsQuantityChange = request.productsQuantityChange();
        return productsQuantityItemUpdater.update(shoppingSessionId, shoppingSessionItemId, productsQuantityChange);
    }
}

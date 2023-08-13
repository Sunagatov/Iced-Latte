package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartManager implements CartApi {

    private final AddItemsToShoppingSessionHelper addItemsToShoppingSessionHelper;
    private final ProductsQuantityItemUpdater productsQuantityItemUpdater;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final ShoppingSessionItemsDeleter shoppingSessionItemsDeleter;

    @Override
    public ShoppingSessionDto getShoppingSessionByUserId(final UUID userId) throws ShoppingSessionNotFoundException {
        return shoppingSessionProvider.getByUserId(userId);
    }

    @Override
    public ShoppingSessionDto addItemsToShoppingSession(final Set<NewShoppingSessionItemDto> items) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, ProductNotFoundException {
        return addItemsToShoppingSessionHelper.add(items);
    }

    @Override
    public ShoppingSessionDto deleteItemsFromShoppingSession(final DeleteItemsFromShoppingSessionRequest deleteItemsFromShoppingSessionRequest) {
        return shoppingSessionItemsDeleter.delete(deleteItemsFromShoppingSessionRequest);
    }

    @Override
    public ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UUID shoppingSessionItemId,
                                                                          final int productsQuantityChange) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdException {
        return productsQuantityItemUpdater.update(shoppingSessionItemId, productsQuantityChange);
    }
}

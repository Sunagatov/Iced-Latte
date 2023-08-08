package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;

import java.util.UUID;

public interface CartApi {

    /**
     * Enables to get ShoppingSession (the cart details)
     *
     * @param userId is the identifier of the user for whom the shopping session is returned
     * @return ShoppingSessionDto (the cart details)
     * */
    ShoppingSessionDto getShoppingSessionByUserId(final UUID userId) throws ShoppingSessionNotFoundException;

    /**
     * Enables to add a new item into the shopping session (the cart details)
     *
     * @param addNewItemToShoppingSessionRequest the request to add a new item into the shopping session (the cart details)
     * @return ShoppingSessionDto (the cart details)
     * */
    ShoppingSessionDto addNewItemToShoppingSession(final AddNewItemToShoppingSessionRequest addNewItemToShoppingSessionRequest);

    /**
     * Enables to remove the specific item from the shopping session (the cart details)
     *
     * @param removeItemsFromShoppingSessionRequest the request to remove the specific item from the shopping session (the cart details)
     * @return ShoppingSessionDto (the shoppingSession details)
     * */
    ShoppingSessionDto removeItemsFromShoppingSession(final RemoveItemsFromShoppingSessionRequest removeItemsFromShoppingSessionRequest);

    /**
     * Enables to change the product's quantity in the specific item of the shopping session (the cart details)
     *
     * @param updateProductsQuantityInShoppingSessionItemRequest the request to change the product's quantity in the specific item of the shopping session (the cart details)
     * @return ShoppingSessionDto (the cart details)
     * @throws ShoppingSessionNotFoundException if there is no ShoppingSession in the database with the provided shoppingSessionId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws ShoppingSessionItemNotFoundException if there is no ShoppingSessionItem in the database with the provided shoppingSessionItemId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws InvalidShoppingSessionIdInUpdateProductsQuantityRequestException if ShoppingSessionId in UpdateProductsQuantityRequest and ShoppingSessionId of Item with id from request are not equal
     * */
    ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest updateProductsQuantityInShoppingSessionItemRequest)
            throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
}

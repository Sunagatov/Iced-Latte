package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.GetShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
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
     * @param userId the identifier of the user for whom the shopping session is returned
     * @return ShoppingSessionDto (the cart details)
     * */
    ShoppingSessionDto getShoppingSession(final UUID userId);

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
     * @param removeItemFromShoppingSessionRequest the request to remove the specific item from the shopping session (the cart details)
     * @return ShoppingSessionDto (the shoppingSession details)
     * */
    ShoppingSessionDto removeItemFromShoppingSession(final RemoveItemFromShoppingSessionRequest removeItemFromShoppingSessionRequest);

    /**
     * Enables to change the product's quantity in the specific item of the shopping session (the cart details)
     *
     * @param userId the identifier of the user for whom the products' quantity of the shopping session item was updated
     * @param shoppingSessionItemId the identifier of the shoppingSessionItem which is going to be updated
     * @param productsQuantityChange the change which we applied for the products' quantity of the shopping session item
     * @return ShoppingSessionDto (the cart details)
     * @throws ShoppingSessionNotFoundException if there is no ShoppingSession in the database with the provided shoppingSessionId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws ShoppingSessionItemNotFoundException if there is no ShoppingSessionItem in the database with the provided shoppingSessionItemId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws InvalidShoppingSessionIdInUpdateProductsQuantityRequestException if ShoppingSessionId in UpdateProductsQuantityRequest and ShoppingSessionId of Item with id from request are not equal
     * */
    ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UUID userId,
                                                                   final UUID shoppingSessionItemId,
                                                                   final Integer productsQuantityChange)
            throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
}

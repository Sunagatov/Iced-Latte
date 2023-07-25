package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.GetShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.product.exception.ProductNotFoundException;

public interface CartApi {

    /**
     * Enables to get ShoppingSession (the cart details)
     *
     * @param getShoppingSessionRequest the request to get the shopping session (the cart details)
     * @return ShoppingSessionDto (the cart details)
     * */
    ShoppingSessionDto getShoppingSession(final GetShoppingSessionRequest getShoppingSessionRequest);

    /**
     * Enables to add a new item into the shopping session (the cart details)
     *
     * @param addNewItemToShoppingSessionRequest the request to add a new item into the shopping session (the cart details)
     * @return ShoppingSessionDto (the cart details)
     * @throws ShoppingSessionNotFoundException if there is no shoppingSession in the database with the provided shoppingSessionId from addNewItemToShoppingSessionRequest
     * @throws ShoppingSessionItemNotFoundException if there is no shoppingSessionItem in the database with the provided shoppingSessionItemId from addNewItemToShoppingSessionRequest
     * @throws ProductNotFoundException if there is no product in the database with the provided productId from addNewItemToShoppingSessionRequest
     * */
    ShoppingSessionDto addNewItemToShoppingSession(final AddNewItemToShoppingSessionRequest addNewItemToShoppingSessionRequest)
            throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, ProductNotFoundException;

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
     * @param updateProductsQuantityInShoppingSessionItemRequest the request to change the product's quantity in the specific item of the shopping session (the cart details)
     * @return ShoppingSessionDto (the cart details)
     * @throws ShoppingSessionNotFoundException if there is no ShoppingSession in the database with the provided shoppingSessionId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws ShoppingSessionItemNotFoundException if there is no ShoppingSessionItem in the database with the provided shoppingSessionItemId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws InvalidShoppingSessionIdInUpdateProductsQuantityRequestException if ShoppingSessionId in UpdateProductsQuantityRequest and ShoppingSessionId of Item with id from request are not equal
     * */
    ShoppingSessionDto updateProductsQuantityInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest updateProductsQuantityInShoppingSessionItemRequest)
            throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
}

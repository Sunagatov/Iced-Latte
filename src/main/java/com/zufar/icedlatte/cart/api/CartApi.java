package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingSessionItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.icedlatte.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;

import java.util.Set;
import java.util.UUID;

public interface CartApi {

    /**
     * Enables to get ShoppingSession (the cart details)
     *
     * @param userId is the identifier of the user for whom the shopping session is returned
     * @return ShoppingSessionDto (the cart details)
     */
    ShoppingSessionDto getShoppingSessionByUserId(final UUID userId) throws ShoppingSessionNotFoundException;

    /**
     * Enables to add a new item into the shopping session (the cart details)
     *
     * @param items which we would like to add to the shoppingSession of the user
     * @return ShoppingSessionDto (the cart details)
     * @throws ShoppingSessionNotFoundException     if there is no shoppingSession in the database with the provided shoppingSessionId from addNewProductToShoppingSessionRequest
     * @throws ShoppingSessionItemNotFoundException if there is no shoppingSessionItem in the database with the provided shoppingSessionItemId from addNewProductToShoppingSessionRequest
     * @throws ProductNotFoundException             if there is no product in the database with the provided productId from addNewProductToShoppingSessionRequest
     */
    ShoppingSessionDto addItemsToShoppingSession(final Set<NewShoppingSessionItemDto> items)
            throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, ProductNotFoundException;

    /**
     * Enables to remove the specific item from the shopping session (the cart details)
     *
     * @param deleteItemsFromShoppingSessionRequest the request to delete the specific items from the shopping session (the cart details)
     * @return ShoppingSessionDto (the shoppingSession details)
     */
    ShoppingSessionDto deleteItemsFromShoppingSession(final DeleteItemsFromShoppingSessionRequest deleteItemsFromShoppingSessionRequest);

    /**
     * Enables to change the product's quantity in the specific item of the shopping session (the cart details)
     *
     * @param shoppingSessionItemId  we use this id to decrease or increase productQuantity of the item with the id = shoppingSessionItemId
     * @param productQuantityChange we use this data to decrease or increase productQuantity of the item with the id = shoppingSessionItemId
     * @return ShoppingSessionDto (the cart details)
     * @throws ShoppingSessionNotFoundException     if there is no ShoppingSession in the database with the provided shoppingSessionId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws ShoppingSessionItemNotFoundException if there is no ShoppingSessionItem in the database with the provided shoppingSessionItemId from updateProductsQuantityInShoppingSessionItemRequest
     * @throws InvalidShoppingSessionIdException    if ShoppingSessionId in UpdateProductsQuantityRequest and ShoppingSessionId of Item with id from request are not equal
     */
    ShoppingSessionDto updateProductQuantityInShoppingSessionItem(final UUID shoppingSessionItemId,
                                                                   final int productQuantityChange)
            throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdException;
}

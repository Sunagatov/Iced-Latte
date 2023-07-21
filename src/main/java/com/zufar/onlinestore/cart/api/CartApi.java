package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.GetShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;

public interface CartApi {

    /**
     * Enables to get ShoppingSession (the Cart details)
     *
     * @param getShoppingSessionRequest the request to get the shopping session (the shoppingSession)
     * @return Shopping session (the shoppingSession details)
     * */
    ShoppingSessionDto getShoppingSession(final GetShoppingSessionRequest getShoppingSessionRequest);

    /**
     * Enables to add a new item into the shopping session (the shoppingSession)
     *
     * @param addNewItemToShoppingSessionRequest the request to add a new item into the shopping session (the shoppingSession)
     * @return Shopping session (the shoppingSession details)
     * */
    ShoppingSessionDto addNewItemToShoppingSession(final AddNewItemToShoppingSessionRequest addNewItemToShoppingSessionRequest);

    /**
     * Enables to remove the specific item from the shopping session (the shoppingSession)
     *
     * @param removeItemFromShoppingSessionRequest the request to remove the specific item from the shopping session (the shoppingSession)
     * @return Shopping session (the shoppingSession details)
     * */
    ShoppingSessionDto removeItemFromShoppingSession(final RemoveItemFromShoppingSessionRequest removeItemFromShoppingSessionRequest);

    /**
     * Enables to change the product's quantity in the specific item of the shopping session (the shoppingSession)
     *
     * @param updateProductsQuantityInShoppingSessionItemRequest the request to change the product's quantity in the specific item of the shopping session (the shoppingSession)
     * @return Shopping session (the shoppingSession details)
     * */
    ShoppingSessionDto updateProductAmountInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest updateProductsQuantityInShoppingSessionItemRequest);
}

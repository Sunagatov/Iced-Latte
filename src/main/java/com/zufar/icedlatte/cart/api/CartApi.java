package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.exception.InvalidShoppingCartIdException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;

import java.util.Set;
import java.util.UUID;

public interface CartApi {

    /**
     * Enables to get ShoppingCart (the cart details)
     *
     * @param userId is the identifier of the user for whom the shopping cart is returned
     * @return ShoppingCartDto (the cart details)
     */
    ShoppingCartDto getShoppingCartByUserId(final UUID userId) throws ShoppingCartNotFoundException;

    /**
     * Enables to add a new item into the shopping cart (the cart details)
     *
     * @param items which we would like to add to the shoppingCart of the user
     * @return ShoppingCartDto (the cart details)
     * @throws ShoppingCartNotFoundException     if there is no shoppingCart in the database with the provided shoppingCartId from addNewProductToShoppingCartRequest
     * @throws ShoppingCartItemNotFoundException if there is no shoppingCartItem in the database with the provided shoppingCartItemId from addNewProductToShoppingCartRequest
     * @throws ProductNotFoundException             if there is no product in the database with the provided productId from addNewProductToShoppingCartRequest
     */
    ShoppingCartDto addItemsToShoppingCart(final Set<NewShoppingCartItemDto> items)
            throws ShoppingCartNotFoundException, ShoppingCartItemNotFoundException, ProductNotFoundException;

    /**
     * Enables to remove the specific item from the shopping cart (the cart details)
     *
     * @param deleteItemsFromShoppingCartRequest the request to delete the specific items from the shopping cart (the cart details)
     * @return ShoppingCartDto (the shoppingCart details)
     */
    ShoppingCartDto deleteItemsFromShoppingCart(final DeleteItemsFromShoppingCartRequest deleteItemsFromShoppingCartRequest);

    /**
     * Enables to change the product's quantity in the specific item of the shopping cart (the cart details)
     *
     * @param shoppingCartItemId  we use this id to decrease or increase productQuantity of the item with the id = shoppingCartItemId
     * @param productQuantityChange we use this data to decrease or increase productQuantity of the item with the id = shoppingCartItemId
     * @return ShoppingCartDto (the cart details)
     * @throws ShoppingCartNotFoundException     if there is no ShoppingCart in the database with the provided shoppingCartId from updateProductsQuantityInShoppingCartItemRequest
     * @throws ShoppingCartItemNotFoundException if there is no ShoppingCartItem in the database with the provided shoppingCartItemId from updateProductsQuantityInShoppingCartItemRequest
     * @throws InvalidShoppingCartIdException    if ShoppingCartId in UpdateProductsQuantityRequest and ShoppingCartId of Item with id from request are not equal
     */
    ShoppingCartDto updateProductQuantityInShoppingCartItem(final UUID shoppingCartItemId,
                                                                   final int productQuantityChange)
            throws ShoppingCartNotFoundException, ShoppingCartItemNotFoundException, InvalidShoppingCartIdException;
}

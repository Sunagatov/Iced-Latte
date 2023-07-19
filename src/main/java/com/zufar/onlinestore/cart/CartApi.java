package com.zufar.onlinestore.cart;

import com.zufar.onlinestore.cart.dto.AddNewItemToShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.GetShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.RemoveItemFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.UpdateProductsQuantityInShoppingSessionItemRequest;

public interface CartApi {

    /**
     * Метод получения покупочной сессии (текущее состояние корзины)
     *
     * @param getShoppingSessionRequest запрос на получение покупочной сессии (текущее состояние корзины)
     * @return покупочная сессия (текущее состояние корзины)
     * */
    ShoppingSessionDto getShoppingSession(final GetShoppingSessionRequest getShoppingSessionRequest);

    /**
     * Метод добавления нового item в покупочную сессию (в корзину)
     *
     * @param addNewItemToShoppingSessionRequest запрос на добавление нового item в покупочную сессию (в корзины)
     * @return покупочная сессия (текущее состояние корзины)
     * */
    ShoppingSessionDto addNewItemToShoppingSession(final AddNewItemToShoppingSessionRequest addNewItemToShoppingSessionRequest);

    /**
     * Метод удаления существующего item в покупочной сессии (в корзине)
     *
     * @param removeItemFromShoppingSessionRequest запрос на удаления существующего item в покупочной сессии (в корзине)
     * @return покупочная сессия (текущее состояние корзины)
     * */
    ShoppingSessionDto removeItemFromShoppingSession(final RemoveItemFromShoppingSessionRequest removeItemFromShoppingSessionRequest);

    /**
     * Метод изменения количества продуктов в определенном item в покупочной сессии (в корзине)
     *
     * @param updateProductsQuantityInShoppingSessionItemRequest запрос на изменение количества продуктов в определенном item в покупочной сессии (в корзине)
     * @return покупочная сессия (текущее состояние корзины)
     * */
    ShoppingSessionDto updateProductAmountInShoppingSessionItem(final UpdateProductsQuantityInShoppingSessionItemRequest updateProductsQuantityInShoppingSessionItemRequest);
}

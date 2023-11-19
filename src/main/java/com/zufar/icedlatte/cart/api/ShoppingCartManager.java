package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.exception.InvalidShoppingCartIdException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartManager implements CartApi {

    private final AddItemsToShoppingCartHelper addItemsToShoppingCartHelper;
    private final ProductQuantityItemUpdater productQuantityItemUpdater;
    private final ShoppingCartProvider shoppingCartProvider;
    private final ShoppingCartItemsDeleter shoppingCartItemsDeleter;

    @Override
    public ShoppingCartDto getShoppingCartByUserId(final UUID userId) throws ShoppingCartNotFoundException {
        return shoppingCartProvider.getByUserId(userId);
    }

    @Override
    public ShoppingCartDto addItemsToShoppingCart(final Set<NewShoppingCartItemDto> items) throws ShoppingCartNotFoundException, ShoppingCartItemNotFoundException, ProductNotFoundException {
        return addItemsToShoppingCartHelper.add(items);
    }

    @Override
    public ShoppingCartDto deleteItemsFromShoppingCart(final DeleteItemsFromShoppingCartRequest deleteItemsFromShoppingCartRequest) {
        return shoppingCartItemsDeleter.delete(deleteItemsFromShoppingCartRequest);
    }

    @Override
    public ShoppingCartDto updateProductQuantityInShoppingCartItem(final UUID shoppingCartItemId,
                                                                          final int productQuantityChange) throws ShoppingCartNotFoundException, ShoppingCartItemNotFoundException, InvalidShoppingCartIdException {
        return productQuantityItemUpdater.update(shoppingCartItemId, productQuantityChange);
    }
}

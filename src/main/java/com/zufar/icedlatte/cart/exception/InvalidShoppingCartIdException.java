package com.zufar.icedlatte.cart.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidShoppingCartIdException extends RuntimeException {

    private final UUID shoppingCartId;

    public InvalidShoppingCartIdException(final UUID shoppingCartId) {
        String format = "The shopping cart id = %s is invalid in UpdateProductsQuantityInShoppingCartItemRequest.";

        super(String.format(format, shoppingCartId));
        this.shoppingCartId = shoppingCartId;
    }
}

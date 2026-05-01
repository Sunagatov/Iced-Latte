package com.zufar.icedlatte.cart.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShoppingCartNotFoundException extends RuntimeException {

    private final UUID userId;

    public ShoppingCartNotFoundException(final UUID userId) {
        String format = "The shopping cart for the user with id = %s is not found.";

        super(String.format(format, userId));
        this.userId = userId;
    }
}

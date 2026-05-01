package com.zufar.icedlatte.cart.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidItemProductQuantityException extends RuntimeException {

    private final Integer itemProductQuantity;

    public InvalidItemProductQuantityException(final Integer itemProductQuantity) {
        super(String.format("Invalid product quantity = %s or product quantity without changes", itemProductQuantity));
        this.itemProductQuantity = itemProductQuantity;
    }
}

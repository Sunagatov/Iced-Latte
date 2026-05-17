package com.zufar.icedlatte.cart.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidItemProductQuantityException extends RuntimeException {

    private final Integer itemProductQuantity;

    public InvalidItemProductQuantityException(final Integer itemProductQuantity,
                                               final int maxItemProductQuantity) {
        super(String.format(
                "Product quantity must be between 1 and %s. Actual value = %s.",
                maxItemProductQuantity,
                itemProductQuantity));
        this.itemProductQuantity = itemProductQuantity;
    }

    public InvalidItemProductQuantityException(final String message) {
        super(message);
        this.itemProductQuantity = null;
    }
}

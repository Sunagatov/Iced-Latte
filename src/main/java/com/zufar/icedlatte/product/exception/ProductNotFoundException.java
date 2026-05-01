package com.zufar.icedlatte.product.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {

    private final List<UUID> productIds;

    public ProductNotFoundException(final UUID productId) {
        super(String.format("The product with productId = %s is not found.", productId));
        this.productIds = List.of(productId);
    }

    public ProductNotFoundException(final List<UUID> productIds) {
        super(String.format("Products with productIds = %s are not found.", productIds.stream().map(UUID::toString)
                .collect(Collectors.joining(", "))));
        this.productIds = productIds;
    }
}

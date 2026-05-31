package com.zufar.icedlatte.favorite.exception;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;

@Getter
public final class FavoriteProductNotFoundException extends FavoriteException {

    private final List<UUID> productIds;

    public FavoriteProductNotFoundException(final List<UUID> productIds) {
        super(String.format(
                "Products with productIds = %s are not found.",
                productIds.stream().map(UUID::toString).collect(Collectors.joining(", "))));
        this.productIds = List.copyOf(productIds);
    }
}

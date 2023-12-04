package com.zufar.icedlatte.favorite.excaption;

import lombok.Getter;

import java.util.UUID;

@Getter
public class FavoritesPageException extends RuntimeException {

    private final UUID userId;

    public FavoritesPageException(final UUID userId) {
        super(String.format("This page is not exist for the user with id = %s.", userId));
        this.userId = userId;
    }
}
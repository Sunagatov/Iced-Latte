package com.zufar.icedlatte.favorite.exception;

public abstract sealed class FavoriteException extends RuntimeException
        permits FavoriteProductNotFoundException, InvalidFavoriteRequestException {

    protected FavoriteException(String message) {
        super(message);
    }
}

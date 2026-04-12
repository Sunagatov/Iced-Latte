package com.zufar.icedlatte.user.exception;

public class InvalidAvatarFileTypeException extends RuntimeException {

    public InvalidAvatarFileTypeException(String contentType) {
        super("Avatar file type not allowed: " + contentType + ". Allowed types: image/jpeg, image/png, image/webp");
    }
}

package com.zufar.icedlatte.user.exception;

import java.util.Collection;

public class InvalidAvatarFileTypeException extends RuntimeException {

    public InvalidAvatarFileTypeException(String contentType,
                                          Collection<String> allowedContentTypes) {
        super("Avatar file type not allowed: " + contentType + ". Allowed types: " + String.join(", ", allowedContentTypes));
    }
}

package com.zufar.icedlatte.email.exception;


public class MessageBuilderNotFoundException extends RuntimeException {

    private final String className;
    public MessageBuilderNotFoundException(String className) {
        super("No message builder found for " + className);
        this.className = className;
    }
}

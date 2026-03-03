package com.zufar.icedlatte.user.exception;

public class PutUsersBadRequestException extends RuntimeException {

    public PutUsersBadRequestException(String errorMessages) {
        super(String.format("PutUsersRequest parameters are incorrect. Error messages are [ %s ].", errorMessages));
    }
}

package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.exception.UserNotFoundException;

import java.util.UUID;

public interface UserApi {

    /**
     * @param saveUserRequest the request to save a new user with the information from saveUserRequest
     * @return UserDto the information about a new user
     */
    UserDto saveUser(final UserDto saveUserRequest);

    /**
     * @param userId the id to get a specific user
     * @return UserDto the information about user with given userId
     * @throws UserNotFoundException if there is no user in the database with the provided userId
     */
    UserDto getUserById(final UUID userId) throws UserNotFoundException;

    /**
     * @param userName the id to get a specific user
     * @return UserDto the information about user with given userName
     * @throws UserNotFoundException if there is no user in the database with the provided userName
     */
    UserDto getUserByUserName(final String userName) throws UserNotFoundException;
}

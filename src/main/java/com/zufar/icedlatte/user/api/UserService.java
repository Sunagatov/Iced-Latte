package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserApi {

    private final SaveUserOperationPerformer saveUserOperationPerformer;
    private final SingleUserProvider singleUserProvider;

    @Override
    public UserDto saveUser(final UserDto userDto) {
        return saveUserOperationPerformer.saveUser(userDto);
    }

    @Override
    public UserDto getUserById(final UUID userId) throws UserNotFoundException {
        return singleUserProvider.getUserById(userId);
    }
}

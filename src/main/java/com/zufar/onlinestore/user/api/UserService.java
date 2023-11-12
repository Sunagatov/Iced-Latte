package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
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
    private final ConfirmUserEmailOperationPerformer confirmUserEmailOperationPerformer;
    private final SendEmailToUserOperationPerformer sendEmailToUserOperationPerformer;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    public UserDto saveUser(final UserDto userDto) {
        return saveUserOperationPerformer.saveUser(userDto);
    }

    @Override
    public UserDto getUserById(final UUID userId) throws UserNotFoundException {
        return singleUserProvider.getUserById(userId);
    }

    @Override
    public void sendEmailConfirmationToken(final UUID userId) throws UserNotFoundException {
        var user = userId != null ? singleUserProvider.getUserById(userId) : securityPrincipalProvider.get();
        if (!user.getEmailConfirmed()) {
            confirmUserEmailOperationPerformer.generateUserEmailConfirmationToken(user.getId());
            sendEmailToUserOperationPerformer.sendUserEmailConfirmationEmail(user.getId());
        }
    }

    @Override
    public void confirmUserEmail(String token) throws UserNotFoundException {
        confirmUserEmailOperationPerformer.confirmUserEmail(token);
    }
}

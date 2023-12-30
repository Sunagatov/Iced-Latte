package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.exception.InvalidTokenException;
import com.zufar.icedlatte.security.api.UserRegistrationService;
import com.zufar.icedlatte.security.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.dto.UserRegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTokenConformer {

    private final TokenManager tokenManager;
    private final UserRegistrationService userRegistrationService;

    public UserRegistrationResponse confirmEmailByCode(ConfirmEmailRequest confirmEmailRequest) {
        final String token = confirmEmailRequest.token();
        final String email = confirmEmailRequest.email();
        UserRegistrationRequest userRegistrationRequest = tokenManager.getToken(token);
        if (token == null || !token.equals(token) ||
                userRegistrationRequest == null || !email.equals(userRegistrationRequest.email())) {
            throw new InvalidTokenException(email);
        }
        tokenManager.removeToken(token);
        return userRegistrationService.register(userRegistrationRequest);
    }
}

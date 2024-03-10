package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.sender.AuthTokenEmailConfirmation;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTokenSender {

    private final AuthTokenEmailConfirmation emailConfirmation;
    private final TokenManager tokenManager;

    public void sendEmailVerificationCode(final UserRegistrationRequest request) {
        String token = tokenManager.generateToken(request);
        emailConfirmation.sendTemporaryCode(request.email(), token);
    }
}

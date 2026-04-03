package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.email.sender.AuthTokenEmailConfirmation;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTokenSender {

    private final AuthTokenEmailConfirmation emailConfirmation;
    private final TokenManager tokenManager;

    public void sendEmailVerificationCode(final UserRegistrationRequest request) {
        String token = tokenManager.generateToken(request, TokenPurpose.EMAIL_VERIFICATION);
        emailConfirmation.sendTemporaryCode(request.getEmail(), token);
    }

    public void sendPasswordResetCode(final String email) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail(email);
        String token = tokenManager.generateToken(request, TokenPurpose.PASSWORD_RESET);
        emailConfirmation.sendTemporaryCode(email, token);
    }
}

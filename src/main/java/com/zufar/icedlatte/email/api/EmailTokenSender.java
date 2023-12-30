package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.sender.EmailConfirmation;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTokenSender {

    private final EmailConfirmation emailConfirmation;
    private final TokenManager tokenManager;
    private final TokenGenerator tokenGenerator;

    public void sendEmailVerificationCode(final UserRegistrationRequest request) {
        String token = tokenGenerator.nextToken();
        tokenManager.addToken(token, request);
        emailConfirmation.sendTemporaryCode(request.email(), token);
    }
}

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
    private final GenerateToken generateToken;

    public void sendEmailVerificationCode(final UserRegistrationRequest request) {
        String token = generateToken.nextToken();
        tokenManager.addToken(token, request);
        emailConfirmation.sendTemporaryCode(request.email(), token);
    }
}

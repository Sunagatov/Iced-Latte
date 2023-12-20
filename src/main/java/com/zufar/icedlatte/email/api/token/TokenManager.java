package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.InvalidTokenException;
import com.zufar.icedlatte.security.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenManager {

    private final TokenCache tokenCache;
    private final TokenGenerator tokenGenerator;
    private final TokenTimeExpirationCache tokenTimeExpirationCache;

    public String generateToken(UserRegistrationRequest request) {
        final String email = request.email();
        tokenTimeExpirationCache.validateTimeToken(email);
        final String token = tokenGenerator.nextToken();
        tokenCache.addToken(token, request);
        tokenTimeExpirationCache.manageEmailSendingRate(email);
        return token;
    }

    public UserRegistrationRequest validateToken(ConfirmEmailRequest confirmEmailRequest) {
        final String token = confirmEmailRequest.token();
        final String requestedEmail = confirmEmailRequest.email();
        UserRegistrationRequest userRegistrationRequest = tokenCache.getToken(token);
        final String savedEmail = userRegistrationRequest.email();

        if (!requestedEmail.equals(savedEmail)) {
            throw new InvalidTokenException(requestedEmail);
        }

        tokenCache.removeToken(token);
        tokenTimeExpirationCache.removeTimeToken(requestedEmail);
        return userRegistrationRequest;
    }
}

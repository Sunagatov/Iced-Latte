package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenManager {

    private final TokenCache tokenCache;
    private final TokenGenerator tokenGenerator;
    private final TokenTimeExpirationCache tokenTimeExpirationCache;

    public String generateToken(final UserRegistrationRequest request, final TokenPurpose purpose) {
        final String email = request.getEmail();
        tokenTimeExpirationCache.validateTimeToken(email);
        final String token = tokenGenerator.nextToken();
        tokenCache.addToken(token, request, purpose);
        tokenTimeExpirationCache.manageEmailSendingRate(email);
        return token;
    }

    public UserRegistrationRequest validateToken(final ConfirmEmailRequest confirmEmailRequest,
                                                 final TokenPurpose expectedPurpose) {
        final String token = confirmEmailRequest.getToken();
        tokenGenerator.tokenIsValid(token);
        return deleteTokenFromCache(token, expectedPurpose);
    }

    public UserRegistrationRequest deleteTokenFromCache(String token, TokenPurpose expectedPurpose) {
        UserRegistrationRequest userRegistrationRequest = tokenCache.getToken(token, expectedPurpose);
        tokenCache.removeToken(token);
        tokenTimeExpirationCache.removeToken(userRegistrationRequest.getEmail());
        return userRegistrationRequest;
    }
}

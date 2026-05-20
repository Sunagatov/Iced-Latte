package com.zufar.icedlatte.security.api.registration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.exception.TimeTokenException;
import com.zufar.icedlatte.security.email.AuthTokenEmailSender;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.api.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String TOKEN_KEY_PREFIX = "email:token:";
    private static final String COOLDOWN_KEY_PREFIX = "email:rate:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExpiringKeyValueStore temporaryStore;
    private final ObjectMapper objectMapper;
    private final AuthTokenEmailSender emailConfirmation;
    private final UserRegistrationService userRegistrationService;
    private final SingleUserProvider singleUserProvider;
    private final UserProfileService userProfileService;

    @Value("${email.verification-token-length}")
    private int tokenLength;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    public void sendEmailVerificationCode(UserRegistrationRequest request) {
        userRegistrationService.ensureEmailAvailable(request);
        String token = generateToken(request, TokenPurpose.EMAIL_VERIFICATION);
        emailConfirmation.sendTemporaryCode(request.getEmail(), token);
    }

    public void sendPasswordResetCode(String email) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail(email);
        String token = generateToken(request, TokenPurpose.PASSWORD_RESET);
        emailConfirmation.sendTemporaryCode(email, token);
    }

    public UserAuthenticationResponse confirmEmailByCode(ConfirmEmailRequest confirmEmailRequest,
                                                         HttpServletRequest httpRequest) {
        UserRegistrationRequest userRegistrationRequest = validateToken(confirmEmailRequest, TokenPurpose.EMAIL_VERIFICATION);
        return userRegistrationService.register(userRegistrationRequest, httpRequest);
    }

    public void confirmResetPasswordEmailByCode(ConfirmEmailRequest confirmEmailRequest,
                                                String newPassword) {
        UserRegistrationRequest request = validateToken(confirmEmailRequest, TokenPurpose.PASSWORD_RESET);
        var userEntity = singleUserProvider.getUserEntityByEmail(request.getEmail());
        userProfileService.changePassword(userEntity.getId(), newPassword);
    }

    public String generateToken(UserRegistrationRequest request, TokenPurpose purpose) {
        String email = request.getEmail();
        validateCooldown(email);
        String token = nextToken();
        Duration ttl = tokenTtl();
        temporaryStore.put(tokenKey(token), serializeEntry(new TokenEntry(request, purpose)), ttl);
        temporaryStore.put(cooldownKey(email), OffsetDateTime.now().plus(ttl).toString(), ttl);
        return token;
    }

    public UserRegistrationRequest validateToken(ConfirmEmailRequest confirmEmailRequest, TokenPurpose expectedPurpose) {
        String token = confirmEmailRequest.getToken();
        validateTokenFormat(token);
        TokenEntry entry = temporaryStore.take(tokenKey(token))
                .map(this::deserializeEntry)
                .orElseThrow(() -> new BadRequestException("Incorrect token"));
        if (entry.purpose() != expectedPurpose) {
            throw new BadRequestException("Incorrect token");
        }
        temporaryStore.remove(cooldownKey(entry.request().getEmail()));
        return entry.request();
    }

    private void validateCooldown(String email) {
        temporaryStore.get(cooldownKey(email))
                .map(OffsetDateTime::parse)
                .ifPresent(expiry -> {
                    throw new TimeTokenException(email, expiry);
                });
    }

    private String serializeEntry(TokenEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize email token entry", e);
        }
    }

    private TokenEntry deserializeEntry(String rawEntry) {
        try {
            return objectMapper.readValue(rawEntry, TokenEntry.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize email token entry", e);
        }
    }

    private String nextToken() {
        return String.format("%0" + tokenLength + "d", RANDOM.nextInt((int) Math.pow(10, tokenLength)));
    }

    private void validateTokenFormat(String token) {
        if (token == null || token.length() != tokenLength || !token.chars().allMatch(Character::isDigit)) {
            throw new BadRequestException("Incorrect token format, token must be " + "#".repeat(tokenLength));
        }
    }

    private Duration tokenTtl() {
        return Duration.ofMinutes(expireTimeMinutes);
    }

    private static String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private static String cooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }

    private record TokenEntry(UserRegistrationRequest request, TokenPurpose purpose) { }
}

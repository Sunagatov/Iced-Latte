package com.zufar.icedlatte.security.signup.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;
import com.zufar.icedlatte.security.signup.registration.UserRegistrationService;
import com.zufar.icedlatte.user.api.UserAccessControlApi;
import com.zufar.icedlatte.user.api.UserLookupApi;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String TOKEN_KEY_PREFIX = "email:token:";
    private static final String COOLDOWN_KEY_PREFIX = "email:rate:";
    private static final String TOKEN_HASH_ALGORITHM = "SHA-256";
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;
    private static final int MIN_TOKEN_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExpiringKeyValueStore temporaryStore;
    private final ObjectMapper objectMapper;
    private final AuthTokenEmailSender emailConfirmation;
    private final UserRegistrationService userRegistrationService;
    private final UserLookupApi userLookupApi;
    private final UserAccessControlApi userAccessControlApi;
    private final PasswordEncoder passwordEncoder;

    @Value("${email.verification-token-length}")
    private int tokenLength;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    public void sendEmailVerificationCode(UserRegistrationRequest request) {
        userRegistrationService.ensureRegistrationAllowed(request);
        String token = generateToken(request, TokenPurpose.EMAIL_VERIFICATION);
        emailConfirmation.sendTemporaryCode(EmailNormalizer.normalize(request.getEmail()), token);
    }

    public void sendPasswordResetCode(String email) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail(EmailNormalizer.normalize(email));
        String token = generateToken(request, TokenPurpose.PASSWORD_RESET);
        emailConfirmation.sendTemporaryCode(request.getEmail(), token);
    }

    public UserAuthenticationResponse confirmEmailByCode(
            ConfirmEmailRequest confirmEmailRequest, HttpServletRequest httpRequest) {
        TokenEntry entry = consumeToken(confirmEmailRequest, TokenPurpose.EMAIL_VERIFICATION);
        if (entry.encodedPassword() == null || entry.encodedPassword().isBlank()) {
            throw new IllegalStateException("Email verification token is missing encoded password");
        }
        return userRegistrationService.completeEmailVerifiedRegistration(entry.request(), entry.encodedPassword(), httpRequest);
    }

    public void confirmResetPasswordEmailByCode(ConfirmEmailRequest confirmEmailRequest, String newPassword) {
        UserRegistrationRequest request = consumeToken(confirmEmailRequest, TokenPurpose.PASSWORD_RESET).request();
        var user = userLookupApi.getUserByEmail(request.getEmail());
        userAccessControlApi.changePassword(user.id(), newPassword);
    }

    public String generateToken(UserRegistrationRequest request, TokenPurpose purpose) {
        String email = EmailNormalizer.normalize(request.getEmail());
        request.setEmail(email);
        validateCooldown(email);
        Duration ttl = tokenTtl();

        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = nextToken();
            String tokenKey = tokenKey(purpose, token);
            String serializedEntry = serializeEntry(new TokenEntry(sanitizedRequest(request), purpose, encodedPassword(request, purpose)));

            if (temporaryStore.putIfAbsent(tokenKey, serializedEntry, ttl)) {
                String value = OffsetDateTime.now().plus(ttl).toString();
                String key = cooldownKey(email);
                temporaryStore.put(key, value, ttl);
                return token;
            }
        }

        throw new IllegalStateException("Failed to allocate unique email token");
    }

    public UserRegistrationRequest validateToken(
            ConfirmEmailRequest confirmEmailRequest, TokenPurpose expectedPurpose) {
        return consumeToken(confirmEmailRequest, expectedPurpose).request();
    }

    private TokenEntry consumeToken(ConfirmEmailRequest confirmEmailRequest, TokenPurpose expectedPurpose) {
        String token = confirmEmailRequest.getToken();
        validateTokenFormat(token);
        TokenEntry entry = temporaryStore
                .take(tokenKey(expectedPurpose, token))
                .map(this::deserializeEntry)
                .orElseThrow(() -> new BadRequestException("Incorrect token"));
        if (entry.purpose() != expectedPurpose) {
            throw new BadRequestException("Incorrect token");
        }
        temporaryStore.remove(cooldownKey(entry.request().getEmail()));
        return entry;
    }

    private void validateCooldown(String email) {
        temporaryStore.get(cooldownKey(email)).map(OffsetDateTime::parse).ifPresent(expiry -> {
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
        validateConfiguredTokenLength();
        byte[] randomBytes = new byte[(int) Math.ceil(tokenLength * 6 / 8.0)];
        RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return token.length() > tokenLength ? token.substring(0, tokenLength) : token;
    }

    private void validateTokenFormat(String token) {
        validateConfiguredTokenLength();
        if (token == null || token.length() != tokenLength || !token.chars().allMatch(this::isUrlSafeTokenChar)) {
            throw new BadRequestException("Incorrect token format");
        }
    }

    private void validateConfiguredTokenLength() {
        if (tokenLength < MIN_TOKEN_LENGTH) {
            throw new IllegalStateException(
                    "email.verification-token-length must be at least " + MIN_TOKEN_LENGTH + ", got: " + tokenLength);
        }
    }

    private boolean isUrlSafeTokenChar(int value) {
        return Character.isLetterOrDigit(value) || value == '-' || value == '_';
    }

    private Duration tokenTtl() {
        validateConfiguredTokenTtl();
        return Duration.ofMinutes(expireTimeMinutes);
    }

    private void validateConfiguredTokenTtl() {
        if (expireTimeMinutes < 1) {
            throw new IllegalStateException("temporary-cache.time.token must be at least 1 minute, got: "
                    + expireTimeMinutes);
        }
    }

    private UserRegistrationRequest sanitizedRequest(UserRegistrationRequest request) {
        UserRegistrationRequest sanitized = new UserRegistrationRequest();
        sanitized.setFirstName(request.getFirstName());
        sanitized.setLastName(request.getLastName());
        sanitized.setBirthDate(request.getBirthDate());
        sanitized.setPhoneNumber(request.getPhoneNumber());
        sanitized.setEmail(request.getEmail());
        sanitized.setAddressDto(request.getAddressDto());
        return sanitized;
    }

    private String encodedPassword(UserRegistrationRequest request, TokenPurpose purpose) {
        if (purpose != TokenPurpose.EMAIL_VERIFICATION) {
            return null;
        }
        return passwordEncoder.encode(request.getPassword());
    }

    private static String tokenKey(TokenPurpose purpose, String token) {
        return TOKEN_KEY_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + hashToken(token);
    }

    private static String hashToken(String token) {
        try {
            byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance(TOKEN_HASH_ALGORITHM).digest(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(TOKEN_HASH_ALGORITHM + " is not available", e);
        }
    }

    private static String cooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }

    private record TokenEntry(UserRegistrationRequest request, TokenPurpose purpose, String encodedPassword) {}
}

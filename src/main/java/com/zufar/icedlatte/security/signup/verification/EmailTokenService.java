package com.zufar.icedlatte.security.signup.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private static final String TOKEN_KEY_PREFIX = "email:token:";
    private static final String COOLDOWN_KEY_PREFIX = "email:rate:";
    private static final String TOKEN_HASH_ALGORITHM = "SHA-256";
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExpiringKeyValueStore temporaryStore;
    private final PasswordEncoder passwordEncoder;
    private final EmailTokenPayloadProtector tokenPayloadProtector;
    private final EmailTokenProperties emailTokenProperties;
    private final TemporaryTokenProperties temporaryTokenProperties;

    public String generate(UserRegistrationRequest request, TokenPurpose purpose) {
        String email = EmailNormalizer.normalize(request.getEmail());
        validateCooldown(email);
        Duration ttl = tokenTtl();

        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = nextToken();
            String tokenKey = tokenKey(purpose, token);
            String encodedPassword = encodedPassword(request, purpose);
            EmailRegistrationPayload registration = registrationPayload(request, purpose, email);
            EmailTokenEntry emailTokenEntry = new EmailTokenEntry(email, registration, purpose, encodedPassword);
            String protectedEntry = tokenPayloadProtector.protect(emailTokenEntry);

            if (temporaryStore.putIfAbsent(tokenKey, protectedEntry, ttl)) {
                String value = OffsetDateTime.now().plus(ttl).toString();
                String key = cooldownKey(email);
                temporaryStore.put(key, value, ttl);
                return token;
            }
        }

        throw new IllegalStateException("Failed to allocate unique email token");
    }

    EmailTokenEntry consume(ConfirmEmailRequest confirmEmailRequest, TokenPurpose expectedPurpose) {
        String token = confirmEmailRequest.getToken();
        validateTokenFormat(token);
        EmailTokenEntry entry = temporaryStore
                .take(tokenKey(expectedPurpose, token))
                .map(tokenPayloadProtector::unprotect)
                .orElseThrow(() -> new BadRequestException("Incorrect token"));
        if (entry.purpose() != expectedPurpose) {
            throw new BadRequestException("Incorrect token");
        }
        temporaryStore.remove(cooldownKey(entry.email()));
        return entry;
    }

    private void validateCooldown(String email) {
        temporaryStore.get(cooldownKey(email)).map(OffsetDateTime::parse).ifPresent(expiry -> {
            throw new TimeTokenException(email, expiry);
        });
    }

    private String nextToken() {
        int tokenLength = emailTokenProperties.verificationTokenLength();
        byte[] randomBytes = new byte[(int) Math.ceil(tokenLength * 6 / 8.0)];
        RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return token.length() > tokenLength ? token.substring(0, tokenLength) : token;
    }

    private void validateTokenFormat(String token) {
        int tokenLength = emailTokenProperties.verificationTokenLength();
        if (token == null || token.length() != tokenLength || !token.chars().allMatch(this::isUrlSafeTokenChar)) {
            throw new BadRequestException("Incorrect token format");
        }
    }

    private boolean isUrlSafeTokenChar(int value) {
        return Character.isLetterOrDigit(value) || value == '-' || value == '_';
    }

    private Duration tokenTtl() {
        return Duration.ofMinutes(temporaryTokenProperties.time().token());
    }

    private EmailRegistrationPayload registrationPayload(
            UserRegistrationRequest request, TokenPurpose purpose, String email) {
        if (purpose != TokenPurpose.EMAIL_VERIFICATION) {
            return null;
        }
        return new EmailRegistrationPayload(request.getFirstName(), request.getLastName(), email);
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
        return COOLDOWN_KEY_PREFIX + hashToken(email);
    }
}

package com.zufar.icedlatte.common.validation.password;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.exception.BadRequestException;

@Component
public class DefaultPasswordPolicy implements PasswordPolicy {
    private static final Set<String> COMMON = Set.of(
            "passwordpassword",
            "passwordpassword1",
            "password123456789",
            "password12345678",
            "123456789012345",
            "1234567890123456",
            "123456789123456789",
            "qwertyuiopasdfgh",
            "qwertyuiopasdfghjkl",
            "iloveyouiloveyou",
            "letmeinletmein123",
            "thisisapassword",
            "correcthorsebatterystaple",
            "icedlattepassword",
            "icedlattepassword1",
            "adminadminadmin",
            "welcome123456789",
            "changemechangeme",
            "password1234567890");

    private static final Set<String> BLOCKED_HASHES = loadBlocklist();

    private static Set<String> loadBlocklist() {
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("security/common-password-sha256.txt").getInputStream(),
                StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            throw new IllegalStateException("Password blocklist unavailable", e);
        }
    }

    private static boolean blocked(String value) {
        try {
            return BLOCKED_HASHES.contains(HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void validateNewPassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 15 || password.length() > 128) {
            throw new BadRequestException("Use a password between 15 and 128 characters.");
        }
        String comparable = password.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (COMMON.contains(comparable)
                || blocked(comparable)
                || comparable.chars().distinct().count() < 4) {
            throw new BadRequestException("Choose a less common password or a longer unique passphrase.");
        }
    }
}

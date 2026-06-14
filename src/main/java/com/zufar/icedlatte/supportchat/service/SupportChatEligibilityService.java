package com.zufar.icedlatte.supportchat.service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatEligibilityService {

    private final SupportChatProperties properties;
    private final UserAuthenticationApi userAuthenticationApi;

    public SupportChatEligibility eligibilityFor(UUID userId) {
        return userAuthenticationApi
                .findUserAuthenticationById(userId)
                .map(this::eligibilityFor)
                .orElseGet(SupportChatEligibility::emailVerificationRequired);
    }

    private SupportChatEligibility eligibilityFor(UserAuthenticationSnapshot user) {
        if (!user.enabled() || !user.accountNonExpired() || !user.accountNonLocked() || !user.credentialsNonExpired()) {
            return SupportChatEligibility.emailVerificationRequired();
        }
        Set<String> allowedEmails = properties.allowedEmails();
        String userEmail = user.email().trim().toLowerCase(Locale.ROOT);

        if (allowedEmails.isEmpty() || allowedEmails.contains(userEmail)) {
            return SupportChatEligibility.createEligible();
        }
        return SupportChatEligibility.accessRestricted();
    }
}

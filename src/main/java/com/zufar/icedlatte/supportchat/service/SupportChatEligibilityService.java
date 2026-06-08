package com.zufar.icedlatte.supportchat.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.user.api.UserAuthenticationApi;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatEligibilityService {

    private final UserAuthenticationApi userAuthenticationApi;

    public SupportChatEligibility eligibilityFor(UUID userId) {
        return userAuthenticationApi
                .findUserAuthenticationById(userId)
                .filter(user -> user.enabled()
                        && user.accountNonExpired()
                        && user.accountNonLocked()
                        && user.credentialsNonExpired())
                .map(_ -> SupportChatEligibility.createEligible())
                .orElseGet(SupportChatEligibility::emailVerificationRequired);
    }
}

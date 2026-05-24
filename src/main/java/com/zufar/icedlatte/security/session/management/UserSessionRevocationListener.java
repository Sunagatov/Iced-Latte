package com.zufar.icedlatte.security.session.management;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.user.api.UserSessionsRevocationRequestedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class UserSessionRevocationListener {

    private final AuthSessionService authSessionService;

    @EventListener
    void on(UserSessionsRevocationRequestedEvent event) {
        authSessionService.revokeAllForUser(event.userId());
    }
}

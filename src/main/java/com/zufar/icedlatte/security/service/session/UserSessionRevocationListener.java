package com.zufar.icedlatte.security.service.session;

import com.zufar.icedlatte.user.api.UserSessionsRevocationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserSessionRevocationListener {

    private final AuthSessionService authSessionService;

    @EventListener
    void on(UserSessionsRevocationRequestedEvent event) {
        authSessionService.revokeAllForUser(event.userId());
    }
}

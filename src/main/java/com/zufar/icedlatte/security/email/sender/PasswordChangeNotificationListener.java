package com.zufar.icedlatte.security.email.sender;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.user.api.UserPasswordChangedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class PasswordChangeNotificationListener {
    private final AuthTokenEmailSender sender;

    @org.springframework.scheduling.annotation.Async("authEmailExecutor")
    @EventListener
    void on(UserPasswordChangedEvent event) {
        try {
            sender.sendPasswordChanged(event.email());
        } catch (RuntimeException e) {
            log.error("auth.password.notification_failed: type={}", e.getClass().getSimpleName());
        }
    }
}

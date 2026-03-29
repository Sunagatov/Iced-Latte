package com.zufar.icedlatte.common.monitoring;

import io.sentry.Sentry;
import io.sentry.protocol.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentryService {

    public void setUser(String userId) {
        User user = new User();
        user.setId(userId);
        Sentry.setUser(user);
    }
}

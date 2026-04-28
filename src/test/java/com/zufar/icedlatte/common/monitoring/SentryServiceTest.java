package com.zufar.icedlatte.common.monitoring;

import io.sentry.Sentry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SentryService unit tests")
class SentryServiceTest {

    @AfterEach
    void tearDown() {
        Sentry.close();
    }

    @Test
    @DisplayName("sets the current Sentry user id")
    void setsCurrentSentryUser() {
        Sentry.init(options -> options.setDsn(""));
        SentryService service = new SentryService();

        service.setUser("user-42");

        Sentry.configureScope(scope -> {
            var user = scope.getUser();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo("user-42");
        });
    }
}

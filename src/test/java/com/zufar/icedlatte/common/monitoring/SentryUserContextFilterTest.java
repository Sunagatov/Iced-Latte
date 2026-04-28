package com.zufar.icedlatte.common.monitoring;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentryUserContextFilter unit tests")
class SentryUserContextFilterTest {

    @Mock private SecurityPrincipalProvider securityPrincipalProvider;
    @Mock private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        io.sentry.Sentry.close();
    }

    @Test
    @DisplayName("sets the Sentry user for the request and clears it afterwards")
    void setsAndClearsSentryUser() throws Exception {
        io.sentry.Sentry.init(options -> options.setDsn(""));
        when(securityPrincipalProvider.getUserId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000042"));
        SentryUserContextFilter filter = new SentryUserContextFilter(securityPrincipalProvider);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        io.sentry.Sentry.configureScope(scope -> assertThat(scope.getUser()).isNull());
    }

    @Test
    @DisplayName("continues the chain when user resolution fails")
    void continuesChainWhenUserResolutionFails() throws Exception {
        io.sentry.Sentry.init(options -> options.setDsn(""));
        doThrow(new IllegalStateException("principal unavailable")).when(securityPrincipalProvider).getUserId();
        SentryUserContextFilter filter = new SentryUserContextFilter(securityPrincipalProvider);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        io.sentry.Sentry.configureScope(scope -> assertThat(scope.getUser()).isNull());
    }
}

package com.zufar.icedlatte.common.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.zufar.icedlatte.common.audit.Identifiable;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentryUserContextFilter unit tests")
class SentryUserContextFilterTest {

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        io.sentry.Sentry.close();
    }

    @Test
    @DisplayName("sets the Sentry user for the request and clears it afterwards")
    void setsAndClearsSentryUser() throws Exception {
        io.sentry.Sentry.init(options -> options.setDsn(""));
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(new TestPrincipal(userId), null));
        SentryUserContextFilter filter = new SentryUserContextFilter();

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        io.sentry.Sentry.configureScope(scope -> assertThat(scope.getUser()).isNull());
    }

    @Test
    @DisplayName("continues the chain when user resolution fails")
    void continuesChainWhenUserResolutionFails() throws Exception {
        io.sentry.Sentry.init(options -> options.setDsn(""));
        SecurityContextHolder.clearContext();
        SentryUserContextFilter filter = new SentryUserContextFilter();

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        io.sentry.Sentry.configureScope(scope -> assertThat(scope.getUser()).isNull());
    }

    private record TestPrincipal(UUID id) implements Identifiable {

        @Override
        public UUID getId() {
            return id;
        }
    }
}

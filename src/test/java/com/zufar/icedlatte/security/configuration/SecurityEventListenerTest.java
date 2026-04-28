package com.zufar.icedlatte.security.configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityEventListener unit tests")
class SecurityEventListenerTest {

    private final SecurityEventListener listener = new SecurityEventListener();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("logs authentication success with the authentication type")
    void logsAuthenticationSuccessWithAuthenticationType() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("alice", "secret");

        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

        assertThat(appender.list)
                .singleElement()
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo("auth.success: authType=TestingAuthenticationToken");
    }

    @Test
    @DisplayName("logs expected anonymous denials at debug level")
    void logsExpectedAnonymousDenialsAtDebugLevel() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/cart");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var event = deniedEvent(new TestingAuthenticationToken("anonymousUser", "n/a"));

        listener.onAuthorizationDenied(event);

        assertThat(appender.list)
                .singleElement()
                .satisfies(loggingEvent -> {
                    assertThat(loggingEvent.getLevel()).isEqualTo(Level.DEBUG);
                    assertThat(loggingEvent.getFormattedMessage())
                            .isEqualTo("auth.denied: method=POST, path=/api/v1/cart, principal=anonymousUser");
                });
    }

    @Test
    @DisplayName("logs unexpected denials at warn level")
    void logsUnexpectedDenialsAtWarnLevel() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var event = deniedEvent(new TestingAuthenticationToken("alice@example.com", "secret"));

        listener.onAuthorizationDenied(event);

        assertThat(appender.list)
                .singleElement()
                .satisfies(loggingEvent -> {
                    assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
                    assertThat(loggingEvent.getFormattedMessage())
                            .isEqualTo("auth.denied: method=GET, path=/api/v1/admin, principal=alice@example.com");
                });
    }

    private static AuthorizationDeniedEvent<Object> deniedEvent(TestingAuthenticationToken authentication) {
        Supplier<org.springframework.security.core.Authentication> supplier = () -> authentication;
        return new AuthorizationDeniedEvent<>(supplier, new Object(), new AuthorizationDecision(false));
    }

    private static ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(SecurityEventListener.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}

package com.zufar.icedlatte.common.audit;

import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditConfig")
class AuditConfigTest {

    private final AuditConfig config = new AuditConfig();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns authenticated user id as auditor")
    void returnsAuthenticatedUserIdAsAuditor() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(user, "credentials");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuditorAware<UUID> auditorProvider = config.auditorProvider();

        assertThat(auditorProvider.getCurrentAuditor()).contains(userId);
    }

    @Test
    @DisplayName("returns empty auditor for anonymous principal")
    void returnsEmptyAuditorForAnonymousPrincipal() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("anonymousUser", "credentials");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(config.auditorProvider().getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("returns empty auditor when principal is not a user entity")
    void returnsEmptyAuditorForUnsupportedPrincipal() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user@example.com", "credentials");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(config.auditorProvider().getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("returns current offset date time")
    void returnsCurrentOffsetDateTime() {
        DateTimeProvider dateTimeProvider = config.dateTimeProvider();

        Object value = dateTimeProvider.getNow().orElseThrow();

        assertThat(value).isInstanceOf(OffsetDateTime.class);
    }
}

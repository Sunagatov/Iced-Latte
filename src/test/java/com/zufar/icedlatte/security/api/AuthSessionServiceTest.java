package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.SessionNotFoundException;
import com.zufar.icedlatte.security.exception.SessionOwnershipException;
import com.zufar.icedlatte.security.repository.AuthSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthSessionService unit tests")
class AuthSessionServiceTest {

    @Mock private AuthSessionRepository sessionRepository;
    @Mock private JwtProperties jwtProperties;
    @Mock private ClientIpExtractor clientIpExtractor;
    @Mock private HttpServletRequest request;
    @InjectMocks private AuthSessionService service;

    @Test
    @DisplayName("createSession saves and returns entity")
    void createSessionSavesEntity() {
        when(jwtProperties.refreshExpiration()).thenReturn(Duration.ofDays(1));
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(clientIpExtractor.extract(request)).thenReturn("127.0.0.1");
        AuthSessionEntity saved = AuthSessionEntity.builder().id(sessionId).userId(userId).build();
        when(sessionRepository.save(any())).thenReturn(saved);

        AuthSessionEntity result = service.createSession(sessionId, userId, "hash123", request);

        assertThat(result.getId()).isEqualTo(sessionId);
        verify(sessionRepository).save(any(AuthSessionEntity.class));
    }

    @Test
    @DisplayName("revokeByRefreshTokenHash revokes existing session")
    void revokeByRefreshTokenHashRevokesSession() {
        AuthSessionEntity session = AuthSessionEntity.builder().id(UUID.randomUUID()).build();
        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.of(session));

        service.revokeByRefreshTokenHash("hash");

        assertThat(session.getRevokedAt()).isNotNull();
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("revokeByRefreshTokenHash does nothing when session not found")
    void revokeByRefreshTokenHashNoOpWhenNotFound() {
        when(sessionRepository.findByRefreshTokenHash("missing")).thenReturn(Optional.empty());
        service.revokeByRefreshTokenHash("missing");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeAllForUser delegates to repository")
    void revokeAllForUserDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        service.revokeAllForUser(userId);
        verify(sessionRepository).revokeAllByUserId(eq(userId), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("revokeById revokes session belonging to user")
    void revokeByIdRevokesOwnSession() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AuthSessionEntity session = AuthSessionEntity.builder().id(sessionId).userId(userId).build();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        service.revokeById(sessionId, userId);

        assertThat(session.getRevokedAt()).isNotNull();
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("revokeById throws when session not found")
    void revokeByIdThrowsWhenNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revokeById(sessionId, UUID.randomUUID()))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("revokeById throws when session belongs to different user")
    void revokeByIdThrowsWhenWrongUser() {
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        AuthSessionEntity session = AuthSessionEntity.builder().id(sessionId).userId(ownerId).build();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.revokeById(sessionId, requesterId))
                .isInstanceOf(SessionOwnershipException.class);
    }

    @Test
    @DisplayName("listActiveSessions delegates to repository")
    void listActiveSessionsDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        List<AuthSessionEntity> sessions = List.of(AuthSessionEntity.builder().id(UUID.randomUUID()).build());
        when(sessionRepository.findActiveSessions(eq(userId), any(OffsetDateTime.class))).thenReturn(sessions);

        assertThat(service.listActiveSessions(userId)).isEqualTo(sessions);
    }

    @Test
    @DisplayName("findActiveByHash throws when previous token hash found (replay attack)")
    void findActiveByHashThrowsOnReplayAttack() {
        AuthSessionEntity compromised = AuthSessionEntity.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).compromised(false).build();
        when(sessionRepository.findByPreviousTokenHash("oldHash")).thenReturn(Optional.of(compromised));

        assertThatThrownBy(() -> service.findActiveByHash("oldHash"))
                .isInstanceOf(JwtTokenBlacklistedException.class)
                .hasMessageContaining("rotated");
    }

    @Test
    @DisplayName("findActiveByHash throws when session not found")
    void findActiveByHashThrowsWhenNotFound() {
        when(sessionRepository.findByPreviousTokenHash("hash")).thenReturn(Optional.empty());
        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findActiveByHash("hash"))
                .isInstanceOf(JwtTokenBlacklistedException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("findActiveByHash throws when session is revoked")
    void findActiveByHashThrowsWhenRevoked() {
        AuthSessionEntity revoked = AuthSessionEntity.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .revokedAt(OffsetDateTime.now().minusHours(1))
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .compromised(false)
                .build();
        when(sessionRepository.findByPreviousTokenHash("hash")).thenReturn(Optional.empty());
        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.findActiveByHash("hash"))
                .isInstanceOf(JwtTokenBlacklistedException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("findActiveByHash throws when session is expired")
    void findActiveByHashThrowsWhenExpired() {
        AuthSessionEntity expired = AuthSessionEntity.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .compromised(false)
                .build();
        when(sessionRepository.findByPreviousTokenHash("hash")).thenReturn(Optional.empty());
        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.findActiveByHash("hash"))
                .isInstanceOf(JwtTokenBlacklistedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("findActiveByHash returns active session")
    void findActiveByHashReturnsActiveSession() {
        AuthSessionEntity active = AuthSessionEntity.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .compromised(false)
                .build();
        when(sessionRepository.findByPreviousTokenHash("hash")).thenReturn(Optional.empty());
        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.of(active));

        assertThat(service.findActiveByHash("hash")).isEqualTo(active);
    }

    @Test
    @DisplayName("rotateSession updates hashes and timestamps")
    void rotateSessionUpdatesSession() {
        when(jwtProperties.refreshExpiration()).thenReturn(Duration.ofDays(1));
        AuthSessionEntity active = AuthSessionEntity.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .compromised(false)
                .build();
        when(sessionRepository.findByPreviousTokenHash("oldHash")).thenReturn(Optional.empty());
        when(sessionRepository.findByRefreshTokenHash("oldHash")).thenReturn(Optional.of(active));

        service.rotateSession("oldHash", "newHash");

        assertThat(active.getRefreshTokenHash()).isEqualTo("newHash");
        assertThat(active.getPreviousTokenHash()).isEqualTo("oldHash");
        verify(sessionRepository, atLeastOnce()).save(active);
    }
}

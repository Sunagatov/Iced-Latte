package com.zufar.icedlatte.security.session.management;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.session.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.session.repository.AuthSessionRepository;
import com.zufar.icedlatte.security.signin.exception.SessionNotFoundException;
import com.zufar.icedlatte.security.signin.exception.SessionOwnershipException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository sessionRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthSessionEntity createSession(
            UUID sessionId, UUID userId, String refreshTokenHash, AuthSessionRequestMetadata requestMetadata) {
        OffsetDateTime now = now();
        AuthSessionEntity session = AuthSessionEntity.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .createdAt(now)
                .expiresAt(expiresAt(now))
                .userAgent(requestMetadata.userAgent())
                .ipAddress(requestMetadata.ipAddress())
                .compromised(false)
                .build();
        sessionRepository.save(session);
        log.info("auth.session.created: userId={}, sessionId={}", userId, maskSessionId(session.getId()));
        return session;
    }

    @Transactional
    public void rotateSession(AuthSessionEntity session, String oldRefreshTokenHash, String newRefreshTokenHash) {
        OffsetDateTime now = now();
        session.setPreviousTokenHash(oldRefreshTokenHash);
        session.setRefreshTokenHash(newRefreshTokenHash);
        session.setLastUsedAt(now);
        session.setExpiresAt(expiresAt(now));
        sessionRepository.save(session);
        log.info("auth.session.rotated: sessionId={}", maskSessionId(session.getId()));
    }

    @Transactional
    public void revokeByRefreshTokenHash(String refreshTokenHash) {
        sessionRepository.findByRefreshTokenHashForUpdate(refreshTokenHash).ifPresent(session -> {
            revokeSession(session);
            log.info("auth.session.revoked: sessionId={}", maskSessionId(session.getId()));
        });
    }

    @Transactional
    public void revokeBySessionId(UUID sessionId) {
        sessionRepository
                .findByIdForUpdate(sessionId)
                .filter(this::isActiveSession)
                .ifPresent(session -> {
                    revokeSession(session);
                    log.info("auth.session.revoked: sessionId={}", maskSessionId(session.getId()));
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(UUID userId) {
        sessionRepository.revokeAllByUserId(userId, now());
        log.info("auth.session.revoked_all: userId={}", userId);
    }

    @Transactional
    public void revokeById(UUID sessionId, UUID requestingUserId) {
        AuthSessionEntity session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!session.getUserId().equals(requestingUserId)) {
            throw new SessionOwnershipException(sessionId);
        }
        revokeSession(session);
        log.info("auth.session.revoked_by_id: sessionId={}, userId={}", maskSessionId(sessionId), requestingUserId);
    }

    @Transactional
    public void revokeAllForCompromisedUserBySessionId(UUID sessionId) {
        // Guard on both revokedAt and compromised: replay path sets both before throwing,
        // so checking only revokedAt would still fire a duplicate revoke_all_compromised.
        sessionRepository
                .findByIdForUpdate(sessionId)
                .filter(this::isActiveSession)
                .ifPresent(s -> revokeAllForCompromisedUser(s.getUserId()));
    }

    public List<AuthSessionEntity> listActiveSessions(UUID userId) {
        return sessionRepository.findActiveSessions(userId, now());
    }

    public List<SessionInfo> listActiveSessionInfos(UUID userId) {
        return listActiveSessions(userId).stream().map(this::toSessionInfo).toList();
    }

    @Transactional
    public AuthSessionEntity findActiveByHash(String refreshTokenHash) {
        Optional<AuthSessionEntity> currentSession =
                sessionRepository.findByRefreshTokenHashForUpdate(refreshTokenHash);
        if (currentSession.isEmpty()) {
            sessionRepository.findByPreviousTokenHashForUpdate(refreshTokenHash).ifPresent(this::handleReplayAttempt);
        }
        AuthSessionEntity session =
                currentSession.orElseThrow(() -> new JwtTokenBlacklistedException("Refresh token not found"));
        if (!isActiveSession(session)) {
            handleRevokedOrCompromisedSession(session);
        }
        if (now().isAfter(session.getExpiresAt())) {
            throw new JwtTokenBlacklistedException("Refresh token has expired");
        }
        return session;
    }

    public void validateActiveSession(UUID sessionId, UUID userId) {
        boolean active = sessionRepository.existsActiveSession(sessionId, userId, now());
        if (!active) {
            throw new JwtTokenBlacklistedException("Session has been revoked");
        }
    }

    private void handleReplayAttempt(AuthSessionEntity session) {
        if (isActiveSession(session)) {
            markCompromised(session);
            String logMessage = "auth.session.replay_detected: sessionId={}, userId={}";
            log.warn(logMessage, maskSessionId(session.getId()), session.getUserId());
            revokeAllForCompromisedUser(session.getUserId());
        } else {
            String logMessage = "auth.session.replay_repeated: sessionId={}, userId={}";
            log.warn(logMessage, maskSessionId(session.getId()), session.getUserId());
        }
        throw new JwtTokenBlacklistedException("Refresh token has been rotated");
    }

    private void handleRevokedOrCompromisedSession(AuthSessionEntity session) {
        if (!session.isCompromised()) {
            markCompromised(session);
            String logMessage = "auth.session.reuse_detected: sessionId={}, userId={}";
            log.warn(logMessage, maskSessionId(session.getId()), session.getUserId());
            revokeAllForCompromisedUser(session.getUserId());
        }
        throw new JwtTokenBlacklistedException("Refresh token has been revoked");
    }

    private boolean isActiveSession(AuthSessionEntity session) {
        return session.getRevokedAt() == null && !session.isCompromised();
    }

    private void markCompromised(AuthSessionEntity session) {
        session.setCompromised(true);
        revokeSession(session);
    }

    private void revokeSession(AuthSessionEntity session) {
        session.setRevokedAt(now());
        sessionRepository.save(session);
    }

    private void revokeAllForCompromisedUser(UUID userId) {
        sessionRepository.markCompromisedAndRevokeAllByUserId(userId, now());
        log.info("auth.session.revoked_all_compromised: userId={}", userId);
    }

    private OffsetDateTime expiresAt(OffsetDateTime now) {
        return now.plus(jwtProperties.refreshExpiration());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now();
    }

    public static String maskSessionId(UUID sessionId) {
        if (sessionId == null) {
            return "unknown";
        }
        String value = sessionId.toString();
        String masked = value.substring(0, Math.min(6, value.length())) + "****";
        return masked.substring(0, Math.min(10, masked.length()));
    }

    private SessionInfo toSessionInfo(AuthSessionEntity session) {
        return new SessionInfo()
                .sessionId(session.getId())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .lastUsedAt(session.getLastUsedAt())
                .userAgent(session.getUserAgent())
                .ipAddress(session.getIpAddress());
    }
}

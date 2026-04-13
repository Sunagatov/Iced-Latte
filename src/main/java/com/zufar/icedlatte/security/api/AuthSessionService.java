package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.SessionNotFoundException;
import com.zufar.icedlatte.security.exception.SessionOwnershipException;
import com.zufar.icedlatte.security.repository.AuthSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository sessionRepository;
    private final JwtProperties jwtProperties;
    private final ClientIpExtractor clientIpExtractor;

    @Transactional
    public AuthSessionEntity createSession(UUID sessionId,
                                           UUID userId,
                                           String refreshTokenHash,
                                           HttpServletRequest request) {
        AuthSessionEntity session = AuthSessionEntity.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plus(jwtProperties.refreshExpiration()))
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(clientIpExtractor.extract(request))
                .compromised(false)
                .build();
        sessionRepository.save(session);
        log.info("auth.session.created: userId={}, sessionId={}", userId, session.getId());
        return session;
    }

    @Transactional
    public void rotateSession(String oldRefreshTokenHash,
                              String newRefreshTokenHash) {
        AuthSessionEntity session = findActiveByHash(oldRefreshTokenHash);
        session.setPreviousTokenHash(oldRefreshTokenHash);
        session.setRefreshTokenHash(newRefreshTokenHash);
        session.setLastUsedAt(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plus(jwtProperties.refreshExpiration()));
        sessionRepository.save(session);
        log.info("auth.session.rotated: sessionId={}", session.getId());
    }

    @Transactional
    public void revokeByRefreshTokenHash(String refreshTokenHash) {
        sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .ifPresent(session -> {
                    session.setRevokedAt(OffsetDateTime.now());
                    sessionRepository.save(session);
                    log.info("auth.session.revoked: sessionId={}", session.getId());
                });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        sessionRepository.revokeAllByUserId(userId, OffsetDateTime.now());
        log.info("auth.session.revoked_all: userId={}", userId);
    }

    @Transactional
    public void revokeById(UUID sessionId,
                           UUID requestingUserId) {
        AuthSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!session.getUserId().equals(requestingUserId)) {
            throw new SessionOwnershipException(sessionId);
        }
        session.setRevokedAt(OffsetDateTime.now());
        sessionRepository.save(session);
        log.info("auth.session.revoked_by_id: sessionId={}, userId={}", sessionId, requestingUserId);
    }

    @Transactional
    public void revokeAllForUserBySessionId(UUID sessionId) {
        // findActiveByHash already revoked all sessions and logged replay_detected.
        // This path handles legacy (non-session-managed) tokens that bypass that flow.
        // Guard on both revokedAt and compromised: replay path sets both before throwing,
        // so checking only revokedAt would still fire a duplicate revoke_all.
        sessionRepository.findById(sessionId)
                .filter(s -> s.getRevokedAt() == null && !s.isCompromised())
                .ifPresent(s -> revokeAllForUser(s.getUserId()));
    }

    public List<AuthSessionEntity> listActiveSessions(UUID userId) {
        return sessionRepository.findActiveSessions(userId, OffsetDateTime.now());
    }

    @Transactional
    public AuthSessionEntity findActiveByHash(String refreshTokenHash) {
        // Check if this is a previously-rotated token (replay attack)
        sessionRepository.findByPreviousTokenHash(refreshTokenHash).ifPresent(session -> {
            if (!session.isCompromised()) {
                session.setCompromised(true);
                session.setRevokedAt(OffsetDateTime.now());
                sessionRepository.save(session);
                log.warn("auth.session.replay_detected: sessionId={}, userId={}", session.getId(), session.getUserId());
                revokeAllForUser(session.getUserId());
            }
            throw new JwtTokenBlacklistedException("Refresh token has been rotated");
        });

        AuthSessionEntity session = sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new JwtTokenBlacklistedException("Refresh token not found"));
        if (session.getRevokedAt() != null || session.isCompromised()) {
            if (!session.isCompromised()) {
                session.setCompromised(true);
                session.setRevokedAt(OffsetDateTime.now());
                sessionRepository.save(session);
                log.warn("auth.session.reuse_detected: sessionId={}, userId={}",
                        session.getId(), session.getUserId());
                revokeAllForUser(session.getUserId());
            }
            throw new JwtTokenBlacklistedException("Refresh token has been revoked");
        }
        if (OffsetDateTime.now().isAfter(session.getExpiresAt())) {
            throw new JwtTokenBlacklistedException("Refresh token has expired");
        }
        return session;
    }
}

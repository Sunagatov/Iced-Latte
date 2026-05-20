package com.zufar.icedlatte.security.api.session;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.SessionNotFoundException;
import com.zufar.icedlatte.security.exception.SessionOwnershipException;
import com.zufar.icedlatte.security.repository.AuthSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
        OffsetDateTime now = now();
        AuthSessionEntity session = AuthSessionEntity.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .createdAt(now)
                .expiresAt(expiresAt(now))
                .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                .ipAddress(clientIpExtractor.extract(request))
                .compromised(false)
                .build();
        sessionRepository.save(session);
        log.info("auth.session.created: userId={}, sessionId={}", userId, maskSessionId(session.getId()));
        return session;
    }

    @Transactional
    public void rotateSession(String oldRefreshTokenHash,
                              String newRefreshTokenHash) {
        AuthSessionEntity session = findActiveByHash(oldRefreshTokenHash);
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
        sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .ifPresent(session -> {
                    revokeSession(session);
                    log.info("auth.session.revoked: sessionId={}", maskSessionId(session.getId()));
                });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        sessionRepository.revokeAllByUserId(userId, now());
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
        revokeSession(session);
        log.info("auth.session.revoked_by_id: sessionId={}, userId={}", maskSessionId(sessionId), requestingUserId);
    }

    @Transactional
    public void revokeAllForUserBySessionId(UUID sessionId) {
        // findActiveByHash already revoked all sessions and logged replay_detected.
        // This path handles legacy (non-session-managed) tokens that bypass that flow.
        // Guard on both revokedAt and compromised: replay path sets both before throwing,
        // so checking only revokedAt would still fire a duplicate revoke_all.
        sessionRepository.findById(sessionId)
                .filter(this::isActiveSession)
                .ifPresent(s -> revokeAllForUser(s.getUserId()));
    }

    public List<AuthSessionEntity> listActiveSessions(UUID userId) {
        return sessionRepository.findActiveSessions(userId, now());
    }

    public List<SessionInfo> listActiveSessionInfos(UUID userId) {
        return listActiveSessions(userId).stream()
                .map(this::toSessionInfo)
                .toList();
    }

    @Transactional
    public AuthSessionEntity findActiveByHash(String refreshTokenHash) {
        sessionRepository.findByPreviousTokenHash(refreshTokenHash)
                .ifPresent(this::handleReplayAttempt);

        AuthSessionEntity session = sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new JwtTokenBlacklistedException("Refresh token not found"));
        if (!isActiveSession(session)) {
            handleRevokedOrCompromisedSession(session);
        }
        if (now().isAfter(session.getExpiresAt())) {
            throw new JwtTokenBlacklistedException("Refresh token has expired");
        }
        return session;
    }

    private void handleReplayAttempt(AuthSessionEntity session) {
        if (isActiveSession(session)) {
            markCompromised(session);
            log.warn("auth.session.replay_detected: sessionId={}, userId={}",
                    maskSessionId(session.getId()), session.getUserId());
            revokeAllForUser(session.getUserId());
        } else {
            log.warn("auth.session.replay_repeated: sessionId={}, userId={}",
                    maskSessionId(session.getId()), session.getUserId());
        }
        throw new JwtTokenBlacklistedException("Refresh token has been rotated");
    }

    private void handleRevokedOrCompromisedSession(AuthSessionEntity session) {
        if (!session.isCompromised()) {
            markCompromised(session);
            log.warn("auth.session.reuse_detected: sessionId={}, userId={}",
                    maskSessionId(session.getId()), session.getUserId());
            revokeAllForUser(session.getUserId());
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

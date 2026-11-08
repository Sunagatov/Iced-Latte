package com.zufar.icedlatte.security.endpoint;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

class RefreshReplayContainmentAuditTest extends AuthenticatedUserIntegrationSupport {
    @org.springframework.beans.factory.annotation.Autowired
    com.zufar.icedlatte.user.repository.UserRepository users;

    @org.springframework.beans.factory.annotation.Autowired
    com.zufar.icedlatte.security.session.repository.AuthSessionRepository sessions;

    @org.springframework.beans.factory.annotation.Autowired
    com.zufar.icedlatte.security.jwt.config.JwtSigningKeys signingKeys;

    @org.springframework.beans.factory.annotation.Autowired
    com.zufar.icedlatte.security.jwt.config.JwtProperties jwtProperties;

    @org.springframework.beans.factory.annotation.Autowired
    com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist blacklist;

    @Test
    void expiredRotatedTokenStillCommitsCompromiseContainment() {
        var user = registerAndAuthenticateUser();
        var userId = users.findByEmail(user.email()).orElseThrow().getId();
        var session = sessions.findAll().stream()
                .filter(value -> value.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
        String expired = io.jsonwebtoken.Jwts.builder()
                .header()
                .keyId(signingKeys.getRefreshKeyId())
                .and()
                .subject(user.email())
                .issuer(jwtProperties.issuer())
                .audience()
                .add(jwtProperties.audience())
                .and()
                .claim(
                        com.zufar.icedlatte.security.jwt.config.JwtClaimNames.TOKEN_PURPOSE,
                        com.zufar.icedlatte.security.jwt.config.JwtClaimNames.REFRESH_TOKEN_PURPOSE)
                .claim(com.zufar.icedlatte.security.jwt.config.JwtClaimNames.VERSION, 2)
                .claim(
                        com.zufar.icedlatte.security.jwt.config.JwtClaimNames.SESSION_ID,
                        session.getId().toString())
                .expiration(java.util.Date.from(java.time.Instant.now().minusSeconds(60)))
                .signWith(signingKeys.getRefresh())
                .compact();
        session.setPreviousTokenHash(blacklist.hash(expired));
        sessions.saveAndFlush(session);
        given(authenticatedJsonSpec("/api/v1/auth", expired))
                .post("/refresh")
                .then()
                .statusCode(401);
        given(authenticatedJsonSpec("/api/v1", user.accessToken()))
                .get("/users")
                .then()
                .statusCode(401);
        given(authenticatedJsonSpec("/api/v1/auth", user.refreshToken()))
                .post("/refresh")
                .then()
                .statusCode(401);
        assertTrue(sessions.findById(session.getId()).orElseThrow().isCompromised());
    }

    @Test
    void replayMustRevokeTheReplacementCredentials() {
        var user = registerAndAuthenticateUser();
        var other = given(jsonSpec("/api/v1/auth"))
                .body(java.util.Map.of("email", user.email(), "password", user.password()))
                .post("/authenticate")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        String otherAccess = other.getString("token");
        var rotated = given(authenticatedJsonSpec("/api/v1/auth", user.refreshToken()))
                .post("/refresh")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        String replacementAccess = rotated.getString("token");
        String replacementRefresh = rotated.getString("refreshToken");
        int replayStatus = given(authenticatedJsonSpec("/api/v1/auth", user.refreshToken()))
                .post("/refresh")
                .statusCode();
        int accessStatus = given(authenticatedJsonSpec("/api/v1", replacementAccess))
                .get("/users")
                .statusCode();
        int refreshStatus = given(authenticatedJsonSpec("/api/v1/auth", replacementRefresh))
                .post("/refresh")
                .statusCode();
        var userId = users.findByEmail(user.email()).orElseThrow().getId();
        var storedSessions = sessions.findAll().stream()
                .filter(session -> session.getUserId().equals(userId))
                .toList();
        assertTrue(storedSessions.size() >= 2);
        assertTrue(
                storedSessions.stream().allMatch(session -> session.isCompromised() && session.getRevokedAt() != null));
        assertEquals(
                401,
                given(authenticatedJsonSpec("/api/v1", otherAccess))
                        .get("/users")
                        .statusCode());
        assertAll(
                () -> assertEquals(401, replayStatus, "Old refresh must be rejected"),
                () -> assertEquals(401, accessStatus, "Detected replay must revoke replacement access"),
                () -> assertEquals(401, refreshStatus, "Detected replay must revoke replacement refresh"));
    }
}

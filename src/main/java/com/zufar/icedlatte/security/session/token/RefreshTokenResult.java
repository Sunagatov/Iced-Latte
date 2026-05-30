package com.zufar.icedlatte.security.session.token;

public record RefreshTokenResult(AuthenticationTokens tokens, boolean migratedLegacyToken) {}

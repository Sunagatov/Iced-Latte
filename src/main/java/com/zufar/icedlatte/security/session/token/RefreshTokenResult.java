package com.zufar.icedlatte.security.session.token;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;

public record RefreshTokenResult(UserAuthenticationResponse response, boolean migratedLegacyToken) {}

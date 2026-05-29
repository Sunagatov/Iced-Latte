package com.zufar.icedlatte.security.jwt.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtClaimNames {

    public static final String JWT_ID = "jti";
    public static final String SESSION_ID = "sid";
    public static final String VERSION = "ver";
    public static final String TOKEN_PURPOSE = "purpose";
    public static final String ACCESS_TOKEN_PURPOSE = "access";
    public static final String REFRESH_TOKEN_PURPOSE = "refresh";
}

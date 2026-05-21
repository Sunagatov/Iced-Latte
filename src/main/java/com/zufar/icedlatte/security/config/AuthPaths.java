package com.zufar.icedlatte.security.config;

import com.zufar.icedlatte.security.api.AuthApiPaths;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthPaths {

    public static final String ROOT = AuthApiPaths.ROOT;
    public static final String REFRESH = AuthApiPaths.REFRESH;
    public static final String LOGOUT_ALL = AuthApiPaths.LOGOUT_ALL;
    public static final String OAUTH = AuthApiPaths.OAUTH;
    public static final String SESSIONS_PATTERN = AuthApiPaths.SESSIONS_PATTERN;
    public static final String ALL_PATTERN = AuthApiPaths.ALL_PATTERN;
}

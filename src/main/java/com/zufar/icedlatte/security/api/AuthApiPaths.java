package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.http.ApiPaths;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthApiPaths {

    public static final String ROOT = ApiPaths.AUTH;
    public static final String ROOT_PREFIX = ApiPaths.AUTH_ROOT_PREFIX;
    public static final String AUTHENTICATE = ApiPaths.AUTH_AUTHENTICATE;
    public static final String REFRESH = ApiPaths.AUTH_REFRESH;
    public static final String LOGOUT_ALL = ApiPaths.AUTH_LOGOUT_ALL;
    public static final String OAUTH = ApiPaths.AUTH_OAUTH;
    public static final String SESSIONS_PATTERN = ApiPaths.AUTH_SESSIONS_PATTERN;
    public static final String ALL_PATTERN = ApiPaths.AUTH_ALL_PATTERN;
}

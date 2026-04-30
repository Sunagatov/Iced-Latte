package com.zufar.icedlatte.security.configuration;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthPaths {

    public static final String ROOT = "/api/v1/auth";
    public static final String ROOT_PREFIX = ROOT + "/";
    public static final String AUTHENTICATE = ROOT + "/authenticate";
    public static final String REFRESH = ROOT + "/refresh";
    public static final String LOGOUT_ALL = ROOT + "/logout-all";
    public static final String GOOGLE = ROOT + "/google";
    public static final String GOOGLE_CALLBACK = GOOGLE + "/callback";
    public static final String SESSIONS_PATTERN = ROOT + "/sessions/**";
    public static final String ALL_PATTERN = ROOT + "/**";
}

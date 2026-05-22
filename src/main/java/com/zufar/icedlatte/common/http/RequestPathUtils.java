package com.zufar.icedlatte.common.http;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestPathUtils {

    public static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    public static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }

    public static boolean isPublicInternetNoise(String path) {
        String normalized = normalizePath(path);
        return !normalized.startsWith(ApiPaths.API_ROOT + "/")
                && !normalized.startsWith(ApiPaths.ACTUATOR_ROOT)
                && !normalized.startsWith(ApiPaths.DOCS_ROOT);
    }
}

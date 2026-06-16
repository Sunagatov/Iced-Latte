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
        return !matchesRootOrNested(normalized, ApiPaths.API_ROOT)
                && !matchesRootOrNested(normalized, ApiPaths.ACTUATOR_ROOT)
                && !matchesRootOrNested(normalized, ApiPaths.DOCS_ROOT);
    }

    public static boolean matchesRootOrNested(String path, String root) {
        String normalizedPath = normalizePath(path);
        String normalizedRoot = normalizePath(root);
        String rootWithoutTrailingSlash = normalizedRoot.endsWith("/") && normalizedRoot.length() > 1
                ? normalizedRoot.substring(0, normalizedRoot.length() - 1)
                : normalizedRoot;
        return normalizedPath.equals(rootWithoutTrailingSlash) || normalizedPath.startsWith(normalizedRoot);
    }
}

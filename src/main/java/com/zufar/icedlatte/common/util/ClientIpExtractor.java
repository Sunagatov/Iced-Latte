package com.zufar.icedlatte.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ClientIpExtractor {

    // Matches only literal IPv4 and IPv6 addresses — no hostnames, no DNS resolution.
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    private static final Pattern IPV6 = Pattern.compile(
            "^[0-9a-fA-F:]+$");

    @Value("${security.trusted-proxies:}")
    private List<String> trustedProxies;

    public String extract(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies != null && trustedProxies.contains(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String firstIp = xForwardedFor.split(",")[0].trim();
                if (isLiteralIp(firstIp)) {
                    return firstIp;
                }
            }
        }
        return remoteAddr;
    }

    /**
     * Shared log-sanitizer: strips CR/LF to prevent log injection.
     * Used by rate-limiting filters so the logic lives in one place.
     */
    public static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n]", "_");
    }

    private static boolean isLiteralIp(String ip) {
        return IPV4.matcher(ip).matches() || IPV6.matcher(ip).matches();
    }
}

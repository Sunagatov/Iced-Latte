package com.zufar.icedlatte.common.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class SessionIdMasker {

    public static String mask(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return "unknown";
        }
        return StringUtils.left(StringUtils.overlay(sessionId, "****", 6, sessionId.length()), 10);
    }
}

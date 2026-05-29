package com.zufar.icedlatte.security.util;

import java.util.Locale;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EmailNormalizer {

    public static String normalize(String email) {
        return email == null ? null : email.toLowerCase(Locale.ROOT).trim();
    }
}

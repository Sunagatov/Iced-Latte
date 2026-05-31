package com.zufar.icedlatte.common.util;

import java.util.Locale;
import java.util.Objects;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EmailNormalizer {

    public static String normalize(String email) {
        return Objects.requireNonNull(email, "email must not be null")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}

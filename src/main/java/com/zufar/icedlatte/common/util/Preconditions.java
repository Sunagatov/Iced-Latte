package com.zufar.icedlatte.common.util;

import java.time.Duration;
import java.util.function.Supplier;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Preconditions {

    public static String requireNotBlankOrThrow(String value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || value.isBlank()) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public static int requirePositiveOrThrow(int value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value <= 0) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public static Duration requirePositiveOrThrow(
            Duration value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw exceptionSupplier.get();
        }
        return value;
    }
}

package com.zufar.icedlatte.common.util;

import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

@UtilityClass
public class Preconditions {

    public static <T> T requireNonNullOrThrow(T obj, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (obj == null) {
            throw exceptionSupplier.get();
        }
        return obj;
    }
}

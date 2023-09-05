package com.zufar.onlinestore.product.util;

import lombok.Getter;

@Getter
public enum ProductPaginationDefaults {

    PAGE(1),
    SIZE(10),
    SORT_ATTRIBUTE("name"),
    SORT_DIRECTION("ASC");

    private final int intValue;

    private final String stringValue;

    ProductPaginationDefaults(int intValue) {
        this.intValue = intValue;
        this.stringValue = null;
    }

    ProductPaginationDefaults(String stringValue) {
        this.stringValue = stringValue;
        this.intValue = -1;
    }
}

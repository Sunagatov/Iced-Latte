package com.zufar.icedlatte.product.api.dto;

import java.util.List;

public record ProductPageSnapshot(
        List<ProductSnapshot> products,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
) {
}

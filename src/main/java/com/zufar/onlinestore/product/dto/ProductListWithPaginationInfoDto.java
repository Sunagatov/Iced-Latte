package com.zufar.onlinestore.product.dto;

import java.util.Collection;

public record ProductListWithPaginationInfoDto(Collection<ProductInfoDto> products,
                                               Integer page,
                                               Integer size,
                                               Long totalElements,
                                               Integer totalPages) {
}
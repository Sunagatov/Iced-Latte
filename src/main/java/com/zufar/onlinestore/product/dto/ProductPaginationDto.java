package com.zufar.onlinestore.product.dto;

import java.util.Collection;

public record ProductPaginationDto(Collection<ProductResponseDto> products,
                                   Integer page,
                                   Integer size,
                                   Long totalElements,
                                   Integer totalPages) {
}
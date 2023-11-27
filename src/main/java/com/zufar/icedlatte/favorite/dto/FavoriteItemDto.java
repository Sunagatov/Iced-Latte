package com.zufar.icedlatte.favorite.dto;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import jakarta.validation.Valid;

import java.util.UUID;

public record FavoriteItemDto(
        UUID id,
        @Valid
        ProductInfoDto productInfo
) {
}
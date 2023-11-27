package com.zufar.icedlatte.favorite.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record FavoriteListDto(
        UUID id,
        @NotBlank(message = "userId is the mandatory attribute")
        UUID userId,
        Set<FavoriteItemDto> favoriteItems,
        OffsetDateTime updatedAt
) {
}
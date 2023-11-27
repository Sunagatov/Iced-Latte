package com.zufar.icedlatte.favorite.dto;


import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record FavoriteListDto(UUID id,
                              UUID userId,
                              Set<FavoriteItemDto> favoriteItems,
                              OffsetDateTime updatedAt) {}
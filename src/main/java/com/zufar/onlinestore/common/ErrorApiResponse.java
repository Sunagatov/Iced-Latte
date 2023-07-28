package com.zufar.onlinestore.common;

import lombok.Builder;
import java.time.Instant;

@Builder
public record ErrorApiResponse(String message,
                               String description,
                               Instant time) {
}
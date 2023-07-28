package com.zufar.onlinestore.common;

import lombok.Builder;
import java.time.Instant;

@Builder
public record ApiResponse(String data,
                          Boolean success,
                          Instant time) {
}
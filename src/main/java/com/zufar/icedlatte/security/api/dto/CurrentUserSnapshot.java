package com.zufar.icedlatte.security.api.dto;

import java.util.UUID;

public record CurrentUserSnapshot(UUID id,
                                  String email) {
}

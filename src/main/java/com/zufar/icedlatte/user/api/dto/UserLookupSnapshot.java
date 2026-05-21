package com.zufar.icedlatte.user.api.dto;

import java.util.UUID;

public record UserLookupSnapshot(UUID id,
                                 String firstName,
                                 String lastName,
                                 String email) {
}

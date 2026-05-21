package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;

import java.util.UUID;

public interface CurrentUserProvider {

    CurrentUserSnapshot get();

    UUID getUserId();
}

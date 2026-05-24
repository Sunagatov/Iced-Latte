package com.zufar.icedlatte.security.api;

import java.util.UUID;

import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;

public interface CurrentUserProvider {

    CurrentUserSnapshot get();

    UUID getUserId();
}

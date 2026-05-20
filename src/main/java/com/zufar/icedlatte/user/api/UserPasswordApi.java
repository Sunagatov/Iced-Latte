package com.zufar.icedlatte.user.api;

import java.util.UUID;

public interface UserPasswordApi {

    void changePassword(UUID userId, String newPassword);
}

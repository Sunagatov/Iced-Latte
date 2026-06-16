package com.zufar.icedlatte.security.api;

import org.jspecify.annotations.NonNull;

public interface SupportChatWebSocketTicketIssuer {

    @NonNull
    String issue(@NonNull String email);
}

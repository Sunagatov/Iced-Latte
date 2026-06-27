package com.zufar.icedlatte.security.api;

public interface SupportChatWebSocketTicketIssuer {

    String issue(String email);
}

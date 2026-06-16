package com.zufar.icedlatte.supportchat.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.security.api.SupportChatWebSocketTicketIssuer;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatWebSocketTicketService {

    private final SupportChatWebSocketTicketIssuer webSocketTicketIssuer;
    private final UserAuthenticationApi userAuthenticationApi;

    public String issue(CurrentUserSnapshot user) {
        String email = userAuthenticationApi
                .findUserAuthenticationById(user.id())
                .map(authentication -> authentication.email().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + user.id()));
        return webSocketTicketIssuer.issue(email);
    }
}

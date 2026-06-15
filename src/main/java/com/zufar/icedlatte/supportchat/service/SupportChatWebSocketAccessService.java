package com.zufar.icedlatte.supportchat.service;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatWebSocketAccessService {

    private final SupportChatAvailabilityService availabilityService;
    private final SupportChatWebSocketTicketService webSocketTicketService;

    public String issueTicket(CurrentUserSnapshot user) {
        availabilityService.requireAvailable(user);
        return webSocketTicketService.issue(user);
    }
}

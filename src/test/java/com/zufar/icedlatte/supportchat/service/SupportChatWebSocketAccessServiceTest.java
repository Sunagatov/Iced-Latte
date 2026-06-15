package com.zufar.icedlatte.supportchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupportChatWebSocketAccessService unit tests")
class SupportChatWebSocketAccessServiceTest {

    @Mock
    private SupportChatAvailabilityService availabilityService;

    @Mock
    private SupportChatWebSocketTicketService webSocketTicketService;

    @Test
    @DisplayName("issues ticket only after support chat availability check passes")
    void issueTicket_availableUser_issuesTicket() {
        CurrentUserSnapshot user = new CurrentUserSnapshot(UUID.randomUUID(), "customer@example.com");
        SupportChatWebSocketAccessService service =
                new SupportChatWebSocketAccessService(availabilityService, webSocketTicketService);
        doNothing().when(availabilityService).requireAvailable(user);
        when(webSocketTicketService.issue(user)).thenReturn("ticket");

        String ticket = service.issueTicket(user);

        assertThat(ticket).isEqualTo("ticket");
        verify(availabilityService).requireAvailable(user);
        verify(webSocketTicketService).issue(user);
    }
}

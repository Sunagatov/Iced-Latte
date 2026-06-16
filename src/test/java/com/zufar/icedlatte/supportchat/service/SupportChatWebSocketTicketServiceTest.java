package com.zufar.icedlatte.supportchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.security.api.SupportChatWebSocketTicketIssuer;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupportChatWebSocketTicketService unit tests")
class SupportChatWebSocketTicketServiceTest {

    @Mock
    private SupportChatWebSocketTicketIssuer webSocketTicketIssuer;

    @Mock
    private UserAuthenticationApi userAuthenticationApi;

    @Test
    @DisplayName("issues ticket using canonical email from current user id")
    void issue_usesCanonicalEmailFromCurrentUserId() {
        UUID userId = UUID.randomUUID();
        CurrentUserSnapshot user = new CurrentUserSnapshot(userId, "stale-session@example.com");
        UserAuthenticationSnapshot authentication = new UserAuthenticationSnapshot(
                userId, "canonical@example.com", "password", java.util.List.of("ROLE_USER"), true, true, true, true);
        SupportChatWebSocketTicketService service =
                new SupportChatWebSocketTicketService(webSocketTicketIssuer, userAuthenticationApi);
        when(userAuthenticationApi.findUserAuthenticationById(userId)).thenReturn(Optional.of(authentication));
        when(webSocketTicketIssuer.issue("canonical@example.com")).thenReturn("ticket");

        String ticket = service.issue(user);

        assertThat(ticket).isEqualTo("ticket");
        verify(userAuthenticationApi).findUserAuthenticationById(userId);
        verify(webSocketTicketIssuer).issue("canonical@example.com");
    }
}

package com.zufar.onlinestore.notification.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.notification.converter.NotificationDtoConverter;
import com.zufar.onlinestore.notification.dto.NotificationDto;
import com.zufar.onlinestore.notification.entity.Notification;
import com.zufar.onlinestore.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;

@WebMvcTest(NotificationEndpoint.class)
class NotificationEndpointTest {

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private NotificationDtoConverter notificationDtoConverter;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String NOTIFICATION_ID = "1";
    private static final String MESSAGE = "This is a notification message";
    private static final Customer RECIPIENT = new Customer(
            "1",
            "FirstName",
            "LastName",
            "mail@mail.ru",
            null
    );

    private static final Notification NOTIFICATION = Notification.builder()
            .id(Long.parseLong(NOTIFICATION_ID))
            .message(MESSAGE)
            .recipient(RECIPIENT)
            .build();

    private static final NotificationDto NOTIFICATION_DTO = new NotificationDto();

    @Test
    @DisplayName("NotificationEndpoint returns HttpStatus 'Created' when createNotification was executed successfully")
    void returnsHttpStatusCreatedWhenCreateNotificationWasCalled() throws Exception {
        Mockito.when(notificationDtoConverter.convertToEntity(NOTIFICATION_DTO))
                .thenReturn(NOTIFICATION);

        Mockito.when(notificationRepository.save(NOTIFICATION))
                .thenReturn(NOTIFICATION);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(NOTIFICATION_DTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    @DisplayName("NotificationEndpoint returns Notification when getNotificationById was called")
    void returnsNotificationWhenGetNotificationByIdWasCalled() throws Exception {
        Mockito.when(notificationRepository.findById(Long.parseLong(NOTIFICATION_ID)))
                .thenReturn(Optional.of(NOTIFICATION));

        Mockito.when(notificationDtoConverter.convertToDto(NOTIFICATION))
                .thenReturn(NOTIFICATION_DTO);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/notifications/{id}", NOTIFICATION_ID)
                        .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("NotificationEndpoint returns HttpStatus 'NotFound' when getNotificationById was called and returned null")
    void returnsNotFoundWhenGetNotificationByIdReturnsNull() throws Exception {
        Mockito.when(notificationRepository.findById(Long.parseLong(NOTIFICATION_ID)))
                .thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/notifications/{id}", NOTIFICATION_ID)
                        .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("NotificationEndpoint returns HttpStatus 'OK' when deleteNotification was called")
    void returnsHttpStatusOkWhenDeleteNotificationWasCalled() throws Exception {
        Mockito.doNothing()
                .when(notificationRepository).deleteById(Long.parseLong(NOTIFICATION_ID));

        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/notifications/{id}", NOTIFICATION_ID)
                        .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("NotificationEndpoint returns HttpStatus 'OK' when updateNotification was called")
    void returnsHttpStatusOkWhenUpdateNotificationWasCalled() throws Exception {
        Mockito.when(notificationDtoConverter.convertToEntity(NOTIFICATION_DTO))
                .thenReturn(NOTIFICATION);

        Mockito.when(notificationRepository.save(NOTIFICATION))
                .thenReturn(NOTIFICATION);

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/notifications/{id}", NOTIFICATION_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(NOTIFICATION_DTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

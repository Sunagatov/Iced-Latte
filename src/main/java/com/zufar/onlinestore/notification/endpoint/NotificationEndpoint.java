package com.zufar.onlinestore.notification.endpoint;

import com.zufar.onlinestore.notification.converter.NotificationDtoConverter;
import com.zufar.onlinestore.notification.dto.NotificationDto;
import com.zufar.onlinestore.notification.entity.Notification;
import com.zufar.onlinestore.notification.repository.NotificationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @author Alex Zarubin
 * created on 27.05.2023
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/notifications")
public class NotificationEndpoint {

    private final NotificationDtoConverter notificationDtoConverter;
    private final NotificationRepository notificationRepository;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> createNotification(@RequestBody @Valid @NotNull(message = "Notification is mandatory") final NotificationDto request) {
        log.info("Received request to create Notification - {}.", request);
        Notification notification = notificationDtoConverter.convertToEntity(request);
        notificationRepository.save(notification);
        log.info("The Notification was created");
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<NotificationDto> getNotificationById(@PathVariable("id") String id) {
        log.info("Received request to get the Notification with id - {}.", id);
        Optional<Notification> notification = notificationRepository.findById(Integer.parseInt(id));
        if (notification.isEmpty()) {
            log.info("the Notification with id - {} is absent.", id);
            return ResponseEntity.notFound()
                    .build();
        }
        NotificationDto notificationDto = notificationDtoConverter.convertToDto(notification.get());
        return ResponseEntity.ok(notificationDto);
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<NotificationDto>> getAllNotifications() {
        log.info("Received request to get all Notifications");
        List<Notification> notifications = notificationRepository.findAll();
        if (notifications.isEmpty()) {
            log.info("Notifications are absent.");
            return ResponseEntity.notFound()
                    .build();
        }
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(notificationDtoConverter::convertToDto)
                .toList();
        log.info("All Notifications were retrieved - {}.", notificationDtos);
        return ResponseEntity.ok(notificationDtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateNotification(@PathVariable("id") String id, @RequestBody @Valid @NotNull(message = "Notification is mandatory") final NotificationDto request) {
        log.info("Received request to update the Notification with id - {}.", id);
        Optional<Notification> notification = notificationRepository.findById(Integer.parseInt(id));
        if (notification.isEmpty()) {
            log.info("The Notification with id - {} is absent.", id);
            return ResponseEntity.notFound()
                    .build();
        }
        Notification notificationToUpdate = notificationDtoConverter.convertToEntity(request);
        notificationToUpdate.setId(Integer.parseInt(id));
        notificationRepository.save(notificationToUpdate);
        log.info("The Notification with id - {} was updated.", id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("id") String id) {
        log.info("Received request to delete the Notification with id - {}.", id);
        notificationRepository.deleteById(Integer.parseInt(id));
        log.info("The Notification with id - {} was deleted.", id);
        return ResponseEntity.ok().build();
    }

}

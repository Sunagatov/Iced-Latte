package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = UserFileUploadEndpoint.API_CUSTOMERS)
public class UserFileUploadEndpoint {

    public static final String API_CUSTOMERS = "/api/v1/users";

    private final FileStorageService fileStorageService;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @PostMapping(path = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(@Validated @RequestParam(value = "file") MultipartFile file) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to upload the user avatar with userId - {}.", userId);
        fileStorageService.uploadUserAvatar(userId, file);
        log.info("The user avatar was uploaded for user with userId - {}.", userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping(path = "/avatar", produces = {MediaType.IMAGE_JPEG_VALUE})
    public ResponseEntity<String> getUserAvatar() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to get the user avatar with userId - {}.", userId);
        String userAvatar = fileStorageService.getUserAvatar(userId);
        log.info("The user avatar was retrieved for user with userId - {}.", userId);
        return ResponseEntity.ok().body(userAvatar);
    }

    @DeleteMapping(path = "/avatar")
    public ResponseEntity<Void> deleteUserAvatar() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to delete the user avatar with userId - {}.", userId);
        fileStorageService.deleteUserAvatar(userId);
        log.info("The user avatar was deleted for user with userId - {}.", userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

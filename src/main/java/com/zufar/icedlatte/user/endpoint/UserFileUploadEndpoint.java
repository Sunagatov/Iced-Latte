package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.avatar.UserAvatarDeleter;
import com.zufar.icedlatte.user.api.avatar.UserAvatarProvider;
import com.zufar.icedlatte.user.api.avatar.UserAvatarUploader;
import com.zufar.icedlatte.common.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = UserFileUploadEndpoint.API_CUSTOMERS)
public class UserFileUploadEndpoint {

    public static final String API_CUSTOMERS = "/api/v1/users";

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final UserAvatarUploader userAvatarUploader;
    private final UserAvatarDeleter userAvatarDeleter;
    private final UserAvatarProvider userAvatarProvider;

    @PostMapping(path = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(@Validated @RequestParam(value = "file") MultipartFile file) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to upload the user avatar with userId - {}.", userId);
        userAvatarUploader.uploadUserAvatar(userId, file);
        log.info("The user avatar was uploaded for user with userId - {}.", userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping(path = "/avatar")
    public ResponseEntity<String> getUserAvatarLink() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to get the user avatar link with userId - {}.", userId);
        String userAvatar = userAvatarProvider.getAvatarUrl(userId);
        log.info("The user avatar link was retrieved for user with userId - {}.", userId);
        return ResponseEntity.ok().body(userAvatar);
    }

    @DeleteMapping(path = "/avatar")
    public ResponseEntity<Void> deleteUserAvatar() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to delete the user avatar with userId - {}.", userId);
        userAvatarDeleter.delete(userId);
        log.info("The user avatar was deleted for user with userId - {}.", userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

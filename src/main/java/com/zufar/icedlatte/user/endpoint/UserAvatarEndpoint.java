package com.zufar.icedlatte.user.endpoint;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.openapi.user.api.UserAvatarApi;
import com.zufar.icedlatte.user.service.UserAvatarUploader;
import com.zufar.icedlatte.user.service.UserProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class UserAvatarEndpoint implements UserAvatarApi {

    private static final String AVATAR_URL = "/api/v1/users/avatar";

    private final UserProfileService userProfileService;
    private final UserAvatarUploader userAvatarUploader;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final HttpServletRequest httpRequest;
    private final ClientIpExtractor clientIpExtractor;

    @Override
    @PostMapping(
            path = AVATAR_URL,
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(
            @RequestPart(value = "file") MultipartFile file,
            @Size(max = 2048) @Valid @RequestParam(value = "turnstileToken", required = false) String turnstileToken) {
        var userId = currentUserId();
        userAvatarUploader.uploadUserAvatar(userId, file, turnstileToken, clientIp());
        log.info("user.avatar.uploaded: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(AVATAR_URL)
    public ResponseEntity<String> getUserAvatarLink() {
        var userId = currentUserId();
        log.debug("user.avatar.get: userId={}", userId);
        return userProfileService
                .findAvatarLink(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @DeleteMapping(AVATAR_URL)
    public ResponseEntity<Void> deleteUserAvatar() {
        var userId = currentUserId();
        userProfileService.deleteAvatar(userId);
        log.info("user.avatar.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    private UUID currentUserId() {
        return currentUserIdProvider.getUserId();
    }

    private String clientIp() {
        return clientIpExtractor.extract(httpRequest);
    }
}

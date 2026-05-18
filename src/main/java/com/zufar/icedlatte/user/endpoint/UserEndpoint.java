package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.security.api.*;
import com.zufar.icedlatte.user.api.*;
import com.zufar.icedlatte.user.api.avatar.UserAvatarUploader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(UserEndpoint.API_CUSTOMERS)
public class UserEndpoint implements com.zufar.icedlatte.openapi.user.api.UserApi {

    public static final String API_CUSTOMERS = ApiPaths.USERS;

    private final UserProfileService userProfileService;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final UserAvatarUploader userAvatarUploader;

    @Override
    @GetMapping
    public ResponseEntity<UserDto> getUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        log.debug("user.profile.get: userId={}", userId);
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @Override
    @PutMapping
    public ResponseEntity<UserDto> editUserProfile(@Valid @RequestBody UpdateUserAccountRequest updateUserAccountRequest) {
        var userId = securityPrincipalProvider.getUserId();
        UserDto updated = userProfileService.updateProfile(userId, updateUserAccountRequest);
        log.info("user.profile.updated: userId={}", userId);
        return ResponseEntity.ok(updated);
    }

    @Override
    @PatchMapping
    public ResponseEntity<Void> changeUserPassword(@Valid @RequestBody ChangeUserPasswordRequest changeUserPasswordRequest) {
        var userId = securityPrincipalProvider.getUserId();
        userProfileService.changePassword(userId, changeUserPasswordRequest);
        log.info("user.password.changed: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        userProfileService.deleteProfile(userId);
        log.info("user.account.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(@RequestPart("file") MultipartFile file) {
        var userId = securityPrincipalProvider.getUserId();
        userAvatarUploader.uploadUserAvatar(userId, file);
        log.info("user.avatar.uploaded: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(path = "/avatar")
    public ResponseEntity<String> getUserAvatarLink() {
        var userId = securityPrincipalProvider.getUserId();
        log.debug("user.avatar.get: userId={}", userId);
        return userProfileService.findAvatarLink(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @DeleteMapping(path = "/avatar")
    public ResponseEntity<Void> deleteUserAvatar() {
        var userId = securityPrincipalProvider.getUserId();
        userProfileService.deleteAvatar(userId);
        log.info("user.avatar.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }
}

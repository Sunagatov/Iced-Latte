package com.zufar.icedlatte.user.endpoint;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.openapi.user.api.UserProfileApi;
import com.zufar.icedlatte.user.service.UserProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class UserProfileEndpoint implements UserProfileApi {

    private static final String USERS_URL = "/api/v1/users";

    private final UserProfileService userProfileService;
    private final CurrentUserIdProvider currentUserIdProvider;

    @Override
    @GetMapping(USERS_URL)
    public ResponseEntity<UserDto> getUserProfile() {
        var userId = currentUserId();
        log.debug("user.profile.get: userId={}", userId);
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @Override
    @PutMapping(USERS_URL)
    public ResponseEntity<UserDto> editUserProfile(
            @Valid @RequestBody UpdateUserAccountRequest updateUserAccountRequest) {
        var userId = currentUserId();
        UserDto updated = userProfileService.updateProfile(userId, updateUserAccountRequest);
        log.info("user.profile.updated: userId={}", userId);
        return ResponseEntity.ok(updated);
    }

    @Override
    @PatchMapping(USERS_URL)
    public ResponseEntity<Void> changeUserPassword(
            @Valid @RequestBody ChangeUserPasswordRequest changeUserPasswordRequest) {
        var userId = currentUserId();
        userProfileService.changePassword(userId, changeUserPasswordRequest);
        log.info("user.password.changed: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping(USERS_URL)
    public ResponseEntity<Void> deleteUserProfile() {
        var userId = currentUserId();
        userProfileService.deleteProfile(userId);
        log.info("user.account.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    private UUID currentUserId() {
        return currentUserIdProvider.getUserId();
    }
}

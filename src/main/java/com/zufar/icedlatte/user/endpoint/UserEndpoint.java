package com.zufar.icedlatte.user.endpoint;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.user.service.DeliveryAddressService;
import com.zufar.icedlatte.user.service.UserAvatarUploader;
import com.zufar.icedlatte.user.service.UserProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(ApiPaths.USERS)
public class UserEndpoint implements com.zufar.icedlatte.openapi.user.api.UserApi {

    private final UserProfileService userProfileService;
    private final UserAvatarUploader userAvatarUploader;
    private final DeliveryAddressService deliveryAddressService;
    private final CurrentUserIdProvider currentUserIdProvider;

    @Override
    @GetMapping
    public ResponseEntity<UserDto> getUserProfile() {
        var userId = currentUserId();
        log.debug("user.profile.get: userId={}", userId);
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @Override
    @PutMapping
    public ResponseEntity<UserDto> editUserProfile(
            @Valid @RequestBody UpdateUserAccountRequest updateUserAccountRequest) {
        var userId = currentUserId();
        UserDto updated = userProfileService.updateProfile(userId, updateUserAccountRequest);
        log.info("user.profile.updated: userId={}", userId);
        return ResponseEntity.ok(updated);
    }

    @Override
    @PatchMapping
    public ResponseEntity<Void> changeUserPassword(
            @Valid @RequestBody ChangeUserPasswordRequest changeUserPasswordRequest) {
        var userId = currentUserId();
        userProfileService.changePassword(userId, changeUserPasswordRequest);
        log.info("user.password.changed: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteUserProfile() {
        var userId = currentUserId();
        userProfileService.deleteProfile(userId);
        log.info("user.account.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(
            path = "/avatar",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "turnstileToken", required = false) String turnstileToken) {
        var userId = currentUserId();
        userAvatarUploader.uploadUserAvatar(userId, file, turnstileToken);
        log.info("user.avatar.uploaded: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(path = "/avatar")
    public ResponseEntity<String> getUserAvatarLink() {
        var userId = currentUserId();
        log.debug("user.avatar.get: userId={}", userId);
        return userProfileService
                .findAvatarLink(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @DeleteMapping(path = "/avatar")
    public ResponseEntity<Void> deleteUserAvatar() {
        var userId = currentUserId();
        userProfileService.deleteAvatar(userId);
        log.info("user.avatar.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/addresses")
    public ResponseEntity<List<DeliveryAddressDto>> getDeliveryAddresses() {
        var userId = currentUserId();
        log.debug("delivery_address.list_requested: userId={}", userId);
        return ResponseEntity.ok(deliveryAddressService.getAll(userId));
    }

    @Override
    @PostMapping("/addresses")
    public ResponseEntity<DeliveryAddressDto> addDeliveryAddress(@Valid @RequestBody DeliveryAddressRequest request) {
        var userId = currentUserId();
        DeliveryAddressDto created = deliveryAddressService.create(userId, request);
        log.info("delivery_address.created: userId={}, addressId={}", userId, created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<DeliveryAddressDto> updateDeliveryAddress(
            @PathVariable UUID addressId, @Valid @RequestBody DeliveryAddressRequest request) {
        var userId = currentUserId();
        DeliveryAddressDto updated = deliveryAddressService.update(userId, addressId, request);
        log.info("delivery_address.updated: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }

    @Override
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteDeliveryAddress(@PathVariable UUID addressId) {
        var userId = currentUserId();
        deliveryAddressService.delete(userId, addressId);
        log.info("delivery_address.deleted: userId={}, addressId={}", userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/addresses/{addressId}/default")
    public ResponseEntity<DeliveryAddressDto> setDefaultDeliveryAddress(@PathVariable UUID addressId) {
        var userId = currentUserId();
        DeliveryAddressDto updated = deliveryAddressService.setDefault(userId, addressId);
        log.info("delivery_address.default_changed: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }

    private UUID currentUserId() {
        return currentUserIdProvider.getUserId();
    }
}

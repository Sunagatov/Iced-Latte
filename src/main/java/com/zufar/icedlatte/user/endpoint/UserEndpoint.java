package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.service.registration.PasswordResetService;
import com.zufar.icedlatte.user.service.DeliveryAddressService;
import com.zufar.icedlatte.user.service.UserProfileService;
import com.zufar.icedlatte.user.service.avatar.UserAvatarUploader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;


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
    private final DeliveryAddressService deliveryAddressService;
    private final PasswordResetService passwordResetService;

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

    @Override
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetUserPassword(@Valid @RequestBody InitiatePasswordResetRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Void> confirmResetUserPassword(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        passwordResetService.confirmReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/addresses")
    public ResponseEntity<List<DeliveryAddressDto>> getDeliveryAddresses() {
        var userId = securityPrincipalProvider.getUserId();
        log.debug("delivery_address.list_requested: userId={}", userId);
        return ResponseEntity.ok(deliveryAddressService.getAll(userId));
    }

    @Override
    @PostMapping("/addresses")
    public ResponseEntity<DeliveryAddressDto> addDeliveryAddress(@Valid @RequestBody DeliveryAddressRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        DeliveryAddressDto created = deliveryAddressService.create(userId, request);
        log.info("delivery_address.created: userId={}, addressId={}", userId, created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<DeliveryAddressDto> updateDeliveryAddress(@PathVariable UUID addressId,
                                                                    @Valid @RequestBody DeliveryAddressRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        DeliveryAddressDto updated = deliveryAddressService.update(userId, addressId, request);
        log.info("delivery_address.updated: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }

    @Override
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteDeliveryAddress(@PathVariable UUID addressId) {
        var userId = securityPrincipalProvider.getUserId();
        deliveryAddressService.delete(userId, addressId);
        log.info("delivery_address.deleted: userId={}, addressId={}", userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/addresses/{addressId}/default")
    public ResponseEntity<DeliveryAddressDto> setDefaultDeliveryAddress(@PathVariable UUID addressId) {
        var userId = securityPrincipalProvider.getUserId();
        DeliveryAddressDto updated = deliveryAddressService.setDefault(userId, addressId);
        log.info("delivery_address.default_changed: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }
}

package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.email.api.*;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.security.api.*;
import com.zufar.icedlatte.user.api.*;
import com.zufar.icedlatte.user.api.avatar.*;
import com.zufar.icedlatte.filestorage.file.*;
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
@RequestMapping(value = UserEndpoint.API_CUSTOMERS)
public class UserEndpoint implements com.zufar.icedlatte.openapi.user.api.UserApi {

    public static final String API_CUSTOMERS = "/api/v1/users";

    private final UpdateUserOperationPerformer updateUserOperationPerformer;
    private final SingleUserProvider singleUserProvider;
    private final ChangeUserPasswordOperationPerformer changeUserPasswordOperationPerformer;
    private final DeleteUserOperationPerformer deleteUserOperationPerformer;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final UserAvatarUploader userAvatarUploader;
    private final FileDeleter fileDeleter;
    private final UserAvatarLinkProvider userAvatarLinkProvider;
    private final EmailTokenConformer emailTokenConformer;

    @Override
    @GetMapping
    public ResponseEntity<UserDto> getUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("user.profile.get: userId={}", userId);
        return ResponseEntity.ok(singleUserProvider.getUserById(userId));
    }

    @Override
    @PutMapping
    public ResponseEntity<UserDto> editUserProfile(@Valid @RequestBody UpdateUserAccountRequest updateUserAccountRequest) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("user.profile.update: userId={}", userId);
        return ResponseEntity.ok(updateUserOperationPerformer.updateUser(updateUserAccountRequest));
    }

    @Override
    @PatchMapping
    public ResponseEntity<Void> changeUserPassword(@Valid @RequestBody ChangeUserPasswordRequest changeUserPasswordRequest) {
        log.info("user.password.change: userId={}", securityPrincipalProvider.getUserId());
        changeUserPasswordOperationPerformer.changeUserPassword(changeUserPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("user.account.delete: userId={}", userId);
        deleteUserOperationPerformer.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(@Validated @RequestPart("file") MultipartFile file) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("user.avatar.upload: userId={}", userId);
        userAvatarUploader.uploadUserAvatar(userId, file);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(path = "/avatar")
    public ResponseEntity<String> getUserAvatarLink() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("user.avatar.get: userId={}", userId);
        return ResponseEntity.ok(userAvatarLinkProvider.getLink(userId));
    }

    @Override
    @DeleteMapping(path = "/avatar")
    public ResponseEntity<Void> deleteUserAvatar() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("user.avatar.delete: userId={}", userId);
        fileDeleter.delete(userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/password/reset")
    public ResponseEntity<Void> resetUserPassword(@Valid @RequestBody InitiatePasswordResetRequest initiatePasswordResetRequest) {
        var user = singleUserProvider.getUserByEmail(initiatePasswordResetRequest.getEmail());
        log.info("user.password.reset: userId={}", user.getId());
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/password/reset/confirm")
    public ResponseEntity<Void> confirmResetUserPassword(@RequestBody final ConfirmPasswordResetRequest confirmEmailRequest) {
        log.info("user.password.reset.confirm");
        emailTokenConformer.confirmResetPasswordEmailByCode(new ConfirmEmailRequest(confirmEmailRequest.getToken()));
        return ResponseEntity.ok().build();
    }

}

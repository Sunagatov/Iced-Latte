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
    private final EmailTokenSender emailTokenSender;

    @Override
    @GetMapping
    public ResponseEntity<UserDto> getUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        log.debug("user.profile.get: userId={}", userId);
        return ResponseEntity.ok(singleUserProvider.getUserById(userId));
    }

    @Override
    @PutMapping
    public ResponseEntity<UserDto> editUserProfile(@Valid @RequestBody UpdateUserAccountRequest updateUserAccountRequest) {
        var userId = securityPrincipalProvider.getUserId();
        UserDto updated = updateUserOperationPerformer.updateUser(updateUserAccountRequest);
        log.info("user.profile.updated: userId={}", userId);
        return ResponseEntity.ok(updated);
    }

    @Override
    @PatchMapping
    public ResponseEntity<Void> changeUserPassword(@Valid @RequestBody ChangeUserPasswordRequest changeUserPasswordRequest) {
        var userId = securityPrincipalProvider.getUserId();
        changeUserPasswordOperationPerformer.changeUserPassword(changeUserPasswordRequest);
        log.info("user.password.changed: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        deleteUserOperationPerformer.deleteUser(userId);
        log.info("user.account.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(@Validated @RequestPart("file") MultipartFile file) {
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
        String link = userAvatarLinkProvider.getLink(userId);
        return link != null ? ResponseEntity.ok(link) : ResponseEntity.notFound().build();
    }

    @Override
    @DeleteMapping(path = "/avatar")
    public ResponseEntity<Void> deleteUserAvatar() {
        var userId = securityPrincipalProvider.getUserId();
        fileDeleter.delete(userId);
        log.info("user.avatar.deleted: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/password/reset")
    public ResponseEntity<Void> resetUserPassword(@Valid @RequestBody InitiatePasswordResetRequest initiatePasswordResetRequest) {
        try {
            singleUserProvider.getUserEntityByEmail(initiatePasswordResetRequest.getEmail());
            emailTokenSender.sendPasswordResetCode(initiatePasswordResetRequest.getEmail());
        } catch (com.zufar.icedlatte.user.exception.UserNotFoundException e) {
            log.warn("user.password.reset.unknown_email");
        } catch (com.zufar.icedlatte.email.exception.TimeTokenException e) {
            // Swallow cooldown error — returning a distinct response would confirm the email exists.
            log.warn("user.password.reset.cooldown");
        }
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/password/reset/confirm")
    public ResponseEntity<Void> confirmResetUserPassword(@Valid @RequestBody final ConfirmPasswordResetRequest confirmEmailRequest) {
        emailTokenConformer.confirmResetPasswordEmailByCode(
                new ConfirmEmailRequest(confirmEmailRequest.getToken()),
                confirmEmailRequest.getNewPassword());
        log.info("user.password.reset.confirmed");
        return ResponseEntity.ok().build();
    }

}

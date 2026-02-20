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
    private final EmailTokenSender emailTokenSender;
    private final EmailTokenConformer emailTokenConformer;

    @Override
    @GetMapping
    public ResponseEntity<UserDto> getUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("Getting user profile: {}", userId);
        var user = singleUserProvider.getUserById(userId);
        log.info("User profile retrieved: {}", userId);
        return ResponseEntity.ok(user);
    }

    @Override
    @PutMapping
    public ResponseEntity<UserDto> editUserProfile(@Valid @RequestBody UpdateUserAccountRequest updateUserAccountRequest) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("Updating user profile: {}", userId);
        var updatedUser = updateUserOperationPerformer.updateUser(updateUserAccountRequest);
        log.info("User profile updated: {}", userId);
        return ResponseEntity.ok(updatedUser);
    }

    @Override
    @PatchMapping
    public ResponseEntity<Void> changeUserPassword(@Valid @RequestBody ChangeUserPasswordRequest changeUserPasswordRequest) {
        // Validate input to prevent code injection
        if (changeUserPasswordRequest.getOldPassword() == null || changeUserPasswordRequest.getOldPassword().trim().isEmpty() ||
            changeUserPasswordRequest.getNewPassword() == null || changeUserPasswordRequest.getNewPassword().trim().isEmpty()) {
            log.warn("Invalid password change request: missing passwords");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Changing user password");
        changeUserPasswordOperationPerformer.changeUserPassword(changeUserPasswordRequest);
        log.info("User password changed");
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteUserProfile() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("Deleting user account: {}", userId);
        deleteUserOperationPerformer.deleteUser(userId);
        log.info("User account deleted: {}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(@Validated @RequestPart("file") MultipartFile file) {
        // Validate file input to prevent code injection
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            log.warn("Invalid file upload: null or empty file");
            return ResponseEntity.badRequest().build();
        }
        
        // Validate file type and size
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("Invalid file type: {}", contentType);
            return ResponseEntity.badRequest().build();
        }
        
        var userId = securityPrincipalProvider.getUserId();
        log.info("Uploading avatar for user: {}", userId);
        userAvatarUploader.uploadUserAvatar(userId, file);
        log.info("Avatar uploaded for user: {}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(path = "/avatar")
    public ResponseEntity<String> getUserAvatarLink() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("Getting avatar link for user: {}", userId);
        var avatarLink = userAvatarLinkProvider.getLink(userId);
        log.info("Avatar link retrieved for user: {}", userId);
        return ResponseEntity.ok(avatarLink);
    }

    @Override
    @DeleteMapping(path = "/avatar")
    public ResponseEntity<Void> deleteUserAvatar() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("Deleting avatar for user: {}", userId);
        fileDeleter.delete(userId);
        log.info("Avatar deleted for user: {}", userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/password/reset")
    public ResponseEntity<Void> resetUserPassword(@Valid @RequestBody InitiatePasswordResetRequest initiatePasswordResetRequest) {
        var user = singleUserProvider.getUserByEmail(initiatePasswordResetRequest.getEmail());
        log.info("Resetting password for user: {}", user.getId());
        var request = new UserRegistrationRequest(user.getFirstName(), user.getLastName(), user.getEmail(), "");
        emailTokenSender.sendEmailVerificationCode(request);
        log.info("Password reset email sent for user: {}", user.getId());
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(path = "/password/reset/confirm")
    public ResponseEntity<Void> confirmResetUserPassword(@RequestBody final ConfirmPasswordResetRequest confirmEmailRequest) {
        log.info("Confirming password reset");
        emailTokenConformer.confirmResetPasswordEmailByCode(new ConfirmEmailRequest(confirmEmailRequest.getToken()));
        log.info("Password reset confirmed");
        return ResponseEntity.ok().build();
    }

}

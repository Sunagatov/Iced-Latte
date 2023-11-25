package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


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
    private final ConfirmUserEmailOperationPerformer confirmUserEmailOperationPerformer;
    private final SendEmailToUserOperationPerformer sendEmailToUserOperationPerformer;

    @Override
    @GetMapping
    public ResponseEntity<UserDto> getUserById() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to get the user with userId - {}.", userId);
        UserDto userDto = singleUserProvider.getUserById(userId);
        log.info("The user with userId - {} was retrieved.", userId);
        return ResponseEntity.ok()
                .body(userDto);
    }

    @Override
    @PutMapping
    public ResponseEntity<UserDto> editUserById(UpdateUserAccountRequest updateUserAccountRequest) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to edit the User with userId - {}.", userId);
        UserDto updatedUserDto = updateUserOperationPerformer.updateUser(updateUserAccountRequest);
        log.info("The user with userId - {} was updated.", userId);
        return ResponseEntity.ok()
                .body(updatedUserDto);
    }

    @Override
    @PatchMapping
    public ResponseEntity<Void> changeUserPassword(ChangeUserPasswordRequest changeUserPasswordRequest) {
        log.info("Received the request to change the user's password.");
        changeUserPasswordOperationPerformer.changeUserPassword(changeUserPasswordRequest);
        log.info("The user's password was changed.");
        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteUserById() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to delete the user's account.");
        deleteUserOperationPerformer.deleteUser(userId);
        log.info("The user's account was deleted.");
        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

    @Override
    @PostMapping("/confirmation")
    public ResponseEntity<Void> postSendUserEmailConfirmation() {
        var user = securityPrincipalProvider.get();
        if (!user.getEmailConfirmed()) {
            confirmUserEmailOperationPerformer.generateUserEmailConfirmationToken(user.getId());
            sendEmailToUserOperationPerformer.sendUserEmailConfirmationEmail(user.getId());
        }
        return ResponseEntity.ok().build();
    }
}

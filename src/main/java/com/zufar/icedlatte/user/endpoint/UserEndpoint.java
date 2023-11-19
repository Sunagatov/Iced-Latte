package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.user.api.UserApi;
import com.zufar.icedlatte.openapi.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserApi userApi;

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable final UUID userId) {
        log.info("Received the request to get the User with userId - {}.", userId);
        UserDto userDto = userApi.getUserById(userId);
        log.info("The user with userId - {} was retrieved.", userId);
        return ResponseEntity.ok()
                .body(userDto);
    }

    @Override
    @PostMapping("/confirmation")
    public ResponseEntity<Void> postSendUserEmailConfirmation() {
        userApi.sendEmailConfirmationToken(null);
        return ResponseEntity.ok().build();
    }
}

package com.zufar.onlinestore.user.endpoint;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.api.UserApi;
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
public class UserEndpoint implements com.zufar.onlinestore.openapi.user.api.UserApi {

    public static final String API_CUSTOMERS = "/api/v1/users";

    private final UserApi userApi;

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable final String userId) {
        log.info("Received the request to get the User with userId - {}.", userId);
        UserDto userDto = userApi.getUserById(UUID.fromString(userId));
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

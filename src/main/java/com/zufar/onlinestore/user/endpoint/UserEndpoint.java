package com.zufar.onlinestore.user.endpoint;

import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.dto.UserDto;
import jakarta.validation.Valid;
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
public class UserEndpoint {

    public static final String API_CUSTOMERS = "/api/v1/users";

    private final UserApi userApi;

    @PostMapping
    public ResponseEntity<UserDto> saveUser(@RequestBody @Valid final UserDto saveUserRequest) {
        log.info("Received saveUserRequest to create User - {}.", saveUserRequest);
        UserDto userDto = userApi.saveUser(saveUserRequest);
        log.info("The User was created");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") final String userId) {
        log.info("Received request to get the User with id - {}.", userId);
        UserDto UserDto = userApi.getUserById(UUID.fromString(userId));
        log.info("the User with id - {} was retrieved - {}.", userId, UserDto);
        return ResponseEntity.ok()
                .body(UserDto);
    }
}

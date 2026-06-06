package com.zufar.icedlatte.security.endpoint;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.security.api.PasswordApi;
import com.zufar.icedlatte.security.signup.password.PasswordResetService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
public class PasswordEndpoint implements PasswordApi {

    private final PasswordResetService passwordResetService;

    @Override
    @PostMapping("/api/v1/auth/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail(), request.getTurnstileToken());
        return ResponseEntity.ok().build();
    }

    @Override
    // amazonq-ignore-next-line
    @PostMapping("/api/v1/auth/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody final ChangePasswordRequest request) {
        passwordResetService.confirmReset(request.getCode(), request.getPassword(), request.getTurnstileToken());
        return ResponseEntity.ok().build();
    }
}

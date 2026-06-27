package com.zufar.icedlatte.user.service;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvatarUploadCompletionHandler {

    private final AvatarUploadCompletionValidator completionValidator;
    private final AvatarUploadSourceObjectValidator sourceObjectValidator;
    private final AvatarUploadLifecycleService lifecycleService;
    private final AvatarUploadActivationService activationService;

    @Transactional
    public Optional<UserAvatarUpload> complete(@Nullable AvatarUploadCompletionCommand command) {
        AvatarUploadCompletion completion = completionValidator.validate(command);
        return lifecycleService.markReady(completion).flatMap(upload -> activationService.activate(upload.getId()));
    }

    @Transactional
    public Optional<UserAvatarUpload> fail(@Nullable AvatarUploadFailureCommand command) {
        if (command == null) {
            throw new BadRequestException("Avatar upload failure is required.");
        }
        ValidAvatarUploadSourceObject source = sourceObjectValidator.validate(command.sourceObject());
        return lifecycleService.markFailed(
                source.uploadId(),
                requiredText(command.failureCode(), "failureCode"),
                requiredText(command.failureMessage(), "failureMessage"));
    }

    private String requiredText(@Nullable String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Avatar upload failure " + fieldName + " is required.");
        }
        return value;
    }
}

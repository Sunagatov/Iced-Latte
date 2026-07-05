package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvatarUploadCompletionHandler unit tests")
class AvatarUploadCompletionHandlerTest {

    private static final UUID USER_ID = UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99");
    private static final UUID UPLOAD_ID = UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431");

    @Mock
    private AvatarUploadCompletionValidator completionValidator;

    @Mock
    private AvatarUploadSourceObjectValidator sourceObjectValidator;

    @Mock
    private AvatarUploadLifecycleService lifecycleService;

    @Mock
    private AvatarUploadActivationService activationService;

    @Test
    @DisplayName("marks completion ready and activates avatar")
    void completeMarksReadyAndActivatesAvatar() {
        AvatarUploadCompletionCommand command = command();
        AvatarUploadCompletion completion = completion();
        UserAvatarUpload ready = upload(UserAvatarUploadStatus.READY);
        UserAvatarUpload active = upload(UserAvatarUploadStatus.READY);
        active.setActive(true);
        AvatarUploadCompletionHandler handler = handler();
        when(completionValidator.validate(command)).thenReturn(completion);
        when(lifecycleService.markReady(completion)).thenReturn(Optional.of(ready));
        when(activationService.activate(UPLOAD_ID)).thenReturn(Optional.of(active));

        Optional<UserAvatarUpload> result = handler.complete(command);

        assertThat(result).containsSame(active);
        verify(lifecycleService).markReady(completion);
        verify(activationService).activate(UPLOAD_ID);
    }

    @Test
    @DisplayName("does not activate when ready transition is ignored")
    void completeDoesNotActivateWhenReadyTransitionIsIgnored() {
        AvatarUploadCompletionCommand command = command();
        AvatarUploadCompletion completion = completion();
        AvatarUploadCompletionHandler handler = handler();
        when(completionValidator.validate(command)).thenReturn(completion);
        when(lifecycleService.markReady(completion)).thenReturn(Optional.empty());

        Optional<UserAvatarUpload> result = handler.complete(command);

        assertThat(result).isEmpty();
        verifyNoInteractions(activationService);
    }

    @Test
    @DisplayName("marks failed upload from Lambda failure payload")
    void failMarksUploadFailed() {
        AvatarUploadFailureCommand command =
                new AvatarUploadFailureCommand(sourceObject(), "DECODE_FAILED", "bad image");
        ValidAvatarUploadSourceObject source = new ValidAvatarUploadSourceObject(
                "iced-latte-users", sourceObject().key(), USER_ID, UPLOAD_ID, "image/png");
        UserAvatarUpload failed = upload(UserAvatarUploadStatus.FAILED);
        AvatarUploadCompletionHandler handler = handler();
        when(sourceObjectValidator.validate(command.sourceObject())).thenReturn(source);
        when(lifecycleService.markFailed(UPLOAD_ID, "DECODE_FAILED", "bad image"))
                .thenReturn(Optional.of(failed));

        Optional<UserAvatarUpload> result = handler.fail(command);

        assertThat(result).containsSame(failed);
        verify(lifecycleService).markFailed(UPLOAD_ID, "DECODE_FAILED", "bad image");
        verifyNoInteractions(activationService);
    }

    @Test
    @DisplayName("rejects null failure payload")
    void failRejectsNullFailurePayload() {
        assertThatThrownBy(() -> handler().fail(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload failure is required.");
    }

    @Test
    @DisplayName("rejects blank failure code")
    void failRejectsBlankFailureCode() {
        AvatarUploadFailureCommand command = new AvatarUploadFailureCommand(sourceObject(), " ", "bad image");
        ValidAvatarUploadSourceObject source = new ValidAvatarUploadSourceObject(
                "iced-latte-users", sourceObject().key(), USER_ID, UPLOAD_ID, "image/png");
        when(sourceObjectValidator.validate(command.sourceObject())).thenReturn(source);

        assertThatThrownBy(() -> handler().fail(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload failure failureCode is required.");

        verifyNoInteractions(lifecycleService, activationService);
    }

    @Test
    @DisplayName("rejects blank failure message")
    void failRejectsBlankFailureMessage() {
        AvatarUploadFailureCommand command = new AvatarUploadFailureCommand(sourceObject(), "DECODE_FAILED", " ");
        ValidAvatarUploadSourceObject source = new ValidAvatarUploadSourceObject(
                "iced-latte-users", sourceObject().key(), USER_ID, UPLOAD_ID, "image/png");
        when(sourceObjectValidator.validate(command.sourceObject())).thenReturn(source);

        assertThatThrownBy(() -> handler().fail(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload failure failureMessage is required.");

        verifyNoInteractions(lifecycleService, activationService);
    }

    private AvatarUploadCompletionHandler handler() {
        return new AvatarUploadCompletionHandler(
                completionValidator, sourceObjectValidator, lifecycleService, activationService);
    }

    private static AvatarUploadCompletionCommand command() {
        return new AvatarUploadCompletionCommand(
                sourceObject(),
                "iced-latte-users",
                "avatars/processed/%s/%s/avatar.webp".formatted(USER_ID, UPLOAD_ID),
                "image/webp",
                384,
                384,
                1024L,
                512L,
                "a".repeat(64));
    }

    private static AvatarUploadCompletion completion() {
        return new AvatarUploadCompletion(
                new ValidAvatarUploadSourceObject(
                        "iced-latte-users", sourceObject().key(), USER_ID, UPLOAD_ID, "image/png"),
                "iced-latte-users",
                "avatars/processed/%s/%s/avatar.webp".formatted(USER_ID, UPLOAD_ID),
                "image/webp",
                384,
                384,
                1024L,
                512L,
                "a".repeat(64));
    }

    private static AvatarUploadSourceObject sourceObject() {
        return new AvatarUploadSourceObject(
                "iced-latte-users",
                "avatars/incoming/%s/%s/source".formatted(USER_ID, UPLOAD_ID),
                Map.of(
                        "upload-id",
                        UPLOAD_ID.toString(),
                        "user-id",
                        USER_ID.toString(),
                        "requested-content-type",
                        "image/png",
                        "avatar-upload-version",
                        "1"));
    }

    private static UserAvatarUpload upload(UserAvatarUploadStatus status) {
        return UserAvatarUpload.builder()
                .id(UPLOAD_ID)
                .userId(USER_ID)
                .status(status)
                .originalBucket("iced-latte-users")
                .originalKey(sourceObject().key())
                .active(false)
                .build();
    }
}

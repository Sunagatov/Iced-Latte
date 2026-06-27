package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AvatarUploadStorageLayout unit tests")
class AvatarUploadStorageLayoutTest {

    private static final UUID USER_ID = UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99");
    private static final UUID UPLOAD_ID = UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431");

    @Test
    @DisplayName("builds incoming source object key from user and upload ids")
    void incomingKeyBuildsExpectedPath() {
        assertThat(AvatarUploadStorageLayout.incomingKey(USER_ID, UPLOAD_ID))
                .isEqualTo("avatars/incoming/%s/%s/source".formatted(USER_ID, UPLOAD_ID));
    }

    @Test
    @DisplayName("builds processed prefix from user and upload ids")
    void processedPrefixBuildsExpectedPath() {
        assertThat(AvatarUploadStorageLayout.processedPrefix(USER_ID, UPLOAD_ID))
                .isEqualTo("avatars/processed/%s/%s/".formatted(USER_ID, UPLOAD_ID));
    }

    @Test
    @DisplayName("builds source metadata with current contract version")
    void sourceMetadataBuildsExpectedEntries() {
        assertThat(AvatarUploadStorageLayout.sourceMetadata(USER_ID, UPLOAD_ID, "image/png"))
                .containsEntry("upload-id", UPLOAD_ID.toString())
                .containsEntry("user-id", USER_ID.toString())
                .containsEntry("requested-content-type", "image/png")
                .containsEntry("avatar-upload-version", "1");
    }
}

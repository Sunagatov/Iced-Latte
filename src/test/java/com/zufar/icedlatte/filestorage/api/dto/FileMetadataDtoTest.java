package com.zufar.icedlatte.filestorage.api.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FileMetadataDto")
class FileMetadataDtoTest {

    @Test
    @DisplayName("rejects missing related object id")
    @SuppressWarnings("DataFlowIssue")
    void rejectsMissingRelatedObjectId() {
        assertThatThrownBy(() -> new FileMetadataDto(null, "bucket", "key"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("rejects blank bucket name")
    void rejectsBlankBucketName() {
        assertThatThrownBy(() -> new FileMetadataDto(UUID.randomUUID(), " ", "key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects blank file name")
    void rejectsBlankFileName() {
        assertThatThrownBy(() -> new FileMetadataDto(UUID.randomUUID(), "bucket", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.zufar.icedlatte.user.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "user_avatar_upload")
public class UserAvatarUpload {

    @Id
    @Column(name = "id", nullable = false)
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @ToString.Include
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @ToString.Include
    private UserAvatarUploadStatus status;

    @Column(name = "original_bucket", nullable = false, length = 128)
    private String originalBucket;

    @Column(name = "original_key", nullable = false, length = 512)
    private String originalKey;

    @Column(name = "processed_bucket", length = 128)
    private String processedBucket;

    @Column(name = "processed_key", length = 512)
    private String processedKey;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "original_size_bytes")
    private Long originalSizeBytes;

    @Column(name = "processed_size_bytes")
    private Long processedSizeBytes;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "sha256", length = 128)
    private String sha256;

    @Column(name = "client_idempotency_key", length = 100)
    private String clientIdempotencyKey;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 512)
    private String failureMessage;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public boolean reusableAt(Instant now) {
        return status == UserAvatarUploadStatus.PENDING_UPLOAD && expiresAt.isAfter(now);
    }
}

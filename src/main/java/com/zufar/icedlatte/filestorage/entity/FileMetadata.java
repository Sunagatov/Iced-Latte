package com.zufar.icedlatte.filestorage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fileId;

    @Column(name = "related_object_id")
    private UUID relatedObjectId;

    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    @Column(name = "file_name", nullable = false, unique = true)
    private String fileName;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

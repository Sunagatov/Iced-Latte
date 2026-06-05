package com.zufar.icedlatte.filestorage.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

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
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

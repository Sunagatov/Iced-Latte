package com.zufar.icedlatte.filestorage.repository;

import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    @Query("SELECT f FROM FileMetadata f WHERE f.relatedObjectId = :relatedObjectId")
    Optional<FileMetadata> findAvatarInfoByRelatedObjectId(@Param("relatedObjectId")  UUID relatedObjectId);

    @Modifying
    @Query("DELETE FROM FileMetadata f WHERE f.relatedObjectId = :relatedObjectId")
    void deleteByRelatedObjectId(@Param("relatedObjectId") UUID relatedObjectId);
}

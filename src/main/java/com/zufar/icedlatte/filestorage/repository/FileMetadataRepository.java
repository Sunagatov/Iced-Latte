package com.zufar.icedlatte.filestorage.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zufar.icedlatte.filestorage.entity.FileMetadata;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    @Query("SELECT f FROM FileMetadata f WHERE f.relatedObjectId IN :relatedObjectIds")
    List<FileMetadata> findByRelatedObjectIdIn(@Param("relatedObjectIds") List<UUID> relatedObjectIds);

    @Modifying
    @Query("DELETE FROM FileMetadata f WHERE f.relatedObjectId = :relatedObjectId")
    int deleteByRelatedObjectId(@Param("relatedObjectId") UUID relatedObjectId);

    @Modifying
    @Query("DELETE FROM FileMetadata f WHERE f.bucketName = :bucketName")
    void deleteByBucketName(@Param("bucketName") String bucketName);
}

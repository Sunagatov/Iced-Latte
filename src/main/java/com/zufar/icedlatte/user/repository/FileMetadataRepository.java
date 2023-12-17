package com.zufar.icedlatte.user.repository;

import com.zufar.icedlatte.common.entity.FileMetadata;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    @Query("SELECT fm FROM FileMetadata fm JOIN UserEntity u ON fm.fileId = u.fileMetadata.fileId WHERE u.id = :userId")
    Optional<FileMetadata> findAvatarInfoByUserId(@Param("userId")  UUID userId);
}

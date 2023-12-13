package com.zufar.icedlatte.user.repository;

import com.zufar.icedlatte.user.entity.AvatarInfo;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvatarInfoRepository extends JpaRepository<AvatarInfo, UUID> {

    @Query("SELECT ai FROM AvatarInfo ai JOIN UserEntity u ON ai.avatarId = u.avatarInfo.avatarId WHERE u.id = :userId")
    Optional<AvatarInfo> findAvatarInfoByUserId(@Param("userId")  UUID userId);
}

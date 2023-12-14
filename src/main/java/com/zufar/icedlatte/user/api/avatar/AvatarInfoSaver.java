package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.converter.AvatarInfoDtoConverter;
import com.zufar.icedlatte.user.dto.AvatarInfoDto;
import com.zufar.icedlatte.user.entity.AvatarInfo;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.AvatarInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarInfoSaver {

    private final AvatarInfoRepository avatarInfoRepository;
    private final AvatarInfoDtoConverter avatarInfoDtoConverter;
    private final SingleUserProvider singleUserProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public AvatarInfoDto save (final AvatarInfoDto avatarInfoDto, final UUID userId) {
        AvatarInfo avatarInfo = avatarInfoDtoConverter.toEntity(avatarInfoDto);
        AvatarInfo savedAvatarInfo = avatarInfoRepository.save(avatarInfo);
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        userEntity.setAvatarInfo(savedAvatarInfo);
        return avatarInfoDtoConverter.toDto(savedAvatarInfo);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public AvatarInfo update (final AvatarInfo avatarInfo, final String newUrl) {
        avatarInfo.setAvatarUrl(newUrl);
        return avatarInfoRepository.save(avatarInfo);
    }
}

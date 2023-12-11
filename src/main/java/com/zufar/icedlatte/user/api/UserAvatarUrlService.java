package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarUrlService {

    private final SingleUserProvider singleUserProvider;
    private final UserDtoConverter userDtoConverter;
    private final UserRepository userCrudRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserDto updateUserUrl(final String avatarUrl, final UUID userId) {
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        userEntity.setAvatarUrl(avatarUrl);
        return userDtoConverter.toDto(userEntity);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public String getAvatarUrlByUserId(final UUID userId) {
        return userCrudRepository.findAvatarUrlByUserId(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteUserAvatar(final UUID userId) {
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        userEntity.setAvatarUrl(null);
    }
}

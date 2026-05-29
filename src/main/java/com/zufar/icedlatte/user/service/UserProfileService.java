package com.zufar.icedlatte.user.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.filestorage.api.FileStorageApi;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserAccessControlApi;
import com.zufar.icedlatte.user.api.UserSessionsRevocationRequestedEvent;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService implements UserAccessControlApi {

    private final SingleUserProvider singleUserProvider;
    private final UserRepository userRepository;
    private final UserDtoConverter userDtoConverter;
    private final PutUsersRequestValidator putUsersRequestValidator;
    private final FileStorageApi fileStorageApi;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        return toProfileDto(singleUserProvider.getUserEntityById(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserDto updateProfile(UUID userId, UpdateUserAccountRequest request) {
        AddressDto addressDto = request.getAddress();
        putUsersRequestValidator.validate(
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber(),
                request.getBirthDate(),
                addressDto);
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        userDtoConverter.updateEntity(userEntity, request);
        return toProfileDto(userRepository.save(userEntity));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteProfile(UUID userId) {
        fileStorageApi.deleteFile(userId);
        userRepository.deleteById(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void changePassword(UUID userId, ChangeUserPasswordRequest request) {
        var userEntity = singleUserProvider.getUserEntityById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), userEntity.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
        changePassword(userId, request.getNewPassword());
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Override
    public void changePassword(UUID userId, String newPassword) {
        userRepository.changeUserPassword(passwordEncoder.encode(newPassword), userId);
        eventPublisher.publishEvent(new UserSessionsRevocationRequestedEvent(userId));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public int lockAccount(String email) {
        return userRepository.setAccountLockedStatus(email, false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public int unlockAccount(String email) {
        return userRepository.setAccountLockedStatus(email, true);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void unlockExpiredAccounts() {
        userRepository.unlockUsers();
    }

    @Transactional(readOnly = true)
    public Optional<String> findAvatarLink(UUID userId) {
        return fileStorageApi.findFileUrl(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteAvatar(UUID userId) {
        fileStorageApi.deleteFile(userId);
    }

    private UserDto toProfileDto(UserEntity userEntity) {
        UserDto userDto = userDtoConverter.toDto(userEntity);
        userDto.setAvatarLink(findAvatarLink(userEntity.getId()).orElse(null));
        return userDto;
    }
}

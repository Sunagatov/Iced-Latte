package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.filestorage.FileStorageService;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.api.AuthSessionService;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import com.zufar.icedlatte.user.validator.PutUsersRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final SingleUserProvider singleUserProvider;
    private final UserRepository userRepository;
    private final UserDtoConverter userDtoConverter;
    private final PutUsersRequestValidator putUsersRequestValidator;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;

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
                addressDto
        );
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        userDtoConverter.updateEntity(userEntity, request);
        return toProfileDto(userRepository.save(userEntity));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteProfile(UUID userId) {
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
    public void changePassword(UUID userId, String newPassword) {
        userRepository.changeUserPassword(passwordEncoder.encode(newPassword), userId);
        authSessionService.revokeAllForUser(userId);
    }

    @Transactional(readOnly = true)
    public Optional<String> findAvatarLink(UUID userId) {
        return fileStorageService.findFileUrl(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteAvatar(UUID userId) {
        fileStorageService.deleteFile(userId);
    }

    private UserDto toProfileDto(UserEntity userEntity) {
        UserDto userDto = userDtoConverter.toDto(userEntity);
        userDto.setAvatarLink(findAvatarLink(userEntity.getId()).orElse(null));
        return userDto;
    }
}

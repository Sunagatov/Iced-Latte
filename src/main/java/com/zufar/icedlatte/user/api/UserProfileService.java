package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.converter.AddressDtoConverter;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import com.zufar.icedlatte.user.validator.PutUsersRequestValidator;
import lombok.RequiredArgsConstructor;
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
    private final AddressDtoConverter addressDtoConverter;
    private final PutUsersRequestValidator putUsersRequestValidator;
    private final FileProvider fileProvider;

    @Transactional(readOnly = true)
    public UserDto getUserProfile(UUID userId) {
        return toUserProfile(singleUserProvider.getUserEntityById(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserDto updateUserProfile(UUID userId, UpdateUserAccountRequest request) {
        AddressDto addressDto = request.getAddress();
        validate(request, addressDto);
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        applyUpdates(userEntity, request, mapAddress(addressDto));
        UserEntity savedUser = userRepository.save(userEntity);
        return toUserProfile(savedUser);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteUserProfile(UUID userId) {
        userRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<String> getAvatarLink(UUID userId) {
        return fileProvider.getRelatedObjectUrl(userId);
    }

    private UserDto toUserProfile(UserEntity userEntity) {
        UserDto userDto = userDtoConverter.toDto(userEntity);
        userDto.setAvatarLink(getAvatarLink(userEntity.getId()).orElse(null));
        return userDto;
    }

    private void validate(UpdateUserAccountRequest request, AddressDto addressDto) {
        putUsersRequestValidator.validate(
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber(),
                request.getBirthDate(),
                addressDto
        );
    }

    private Address mapAddress(AddressDto addressDto) {
        return isAddressEmpty(addressDto) ? null : addressDtoConverter.toEntity(addressDto);
    }

    private void applyUpdates(UserEntity userEntity,
                              UpdateUserAccountRequest request,
                              Address address) {
        userEntity.setFirstName(request.getFirstName());
        userEntity.setLastName(request.getLastName());
        userEntity.setBirthDate(request.getBirthDate());
        userEntity.setPhoneNumber(request.getPhoneNumber());
        userEntity.setAddress(address);
    }

    private boolean isAddressEmpty(AddressDto addressDto) {
        if (addressDto == null) {
            return true;
        }
        return isBlank(addressDto.getCountry())
                && isBlank(addressDto.getCity())
                && isBlank(addressDto.getLine())
                && isBlank(addressDto.getPostcode());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

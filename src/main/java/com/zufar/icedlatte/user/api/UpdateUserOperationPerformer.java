package com.zufar.icedlatte.user.api;

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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateUserOperationPerformer {

    private final SingleUserProvider singleUserProvider;
    private final UserRepository userCrudRepository;
    private final UserDtoConverter userDtoConverter;
    private final AddressDtoConverter addressDtoConverter;
    private final PutUsersRequestValidator putUsersRequestValidator;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserDto updateUser(final UUID userId, final UpdateUserAccountRequest updateUserAccountRequest) {
        AddressDto addressDto = updateUserAccountRequest.getAddress();

        putUsersRequestValidator.validate(
                updateUserAccountRequest.getFirstName(),
                updateUserAccountRequest.getLastName(),
                updateUserAccountRequest.getPhoneNumber(),
                updateUserAccountRequest.getBirthDate(),
                addressDto
        );

        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        Address addressEntity = isAddressEmpty(addressDto) ? null : addressDtoConverter.toEntity(addressDto);

        userEntity.setFirstName(updateUserAccountRequest.getFirstName());
        userEntity.setLastName(updateUserAccountRequest.getLastName());
        userEntity.setBirthDate(updateUserAccountRequest.getBirthDate());
        userEntity.setPhoneNumber(updateUserAccountRequest.getPhoneNumber());
        userEntity.setAddress(addressEntity);

        UserEntity userEntityWithId = userCrudRepository.save(userEntity);
        return userDtoConverter.toDto(userEntityWithId);
    }

    private boolean isAddressEmpty(AddressDto addressDto) {
        if (addressDto == null) return true;
        return isBlank(addressDto.getCountry())
                && isBlank(addressDto.getCity())
                && isBlank(addressDto.getLine())
                && isBlank(addressDto.getPostcode());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

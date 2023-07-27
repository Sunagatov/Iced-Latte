package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.user.dto.AddressDto;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.Address;
import com.zufar.onlinestore.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserDtoConverter {
    private final AddressDtoConverter addressDtoConverter;

    public UserDto toDto(final UserEntity entity) {
        AddressDto addressDto = addressDtoConverter.toDto(entity.getAddress());
        return new UserDto(
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPassword(),
                addressDto
        );
    }

    public UserEntity toEntity(final UserDto dto) {
        Address address = addressDtoConverter.toEntity(dto.address());
        return UserEntity.builder()
                .userId(dto.userId())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email())
                .address(address)
                .build();
    }
}

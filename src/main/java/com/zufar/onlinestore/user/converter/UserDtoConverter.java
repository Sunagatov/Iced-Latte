package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.user.dto.AddressDto;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.Address;
import com.zufar.onlinestore.user.entity.User;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserDtoConverter {
    private final AddressDtoConverter addressDtoConverter;

    public UserDto toDto(final User entity) {
        AddressDto addressDto = addressDtoConverter.toDto(entity.getAddress());
        return new UserDto(
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                addressDto
        );
    }

    public User toEntity(final UserDto dto) {
        Address address = addressDtoConverter.toEntity(dto.address());
        return User.builder()
                .userId(dto.userId())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email())
                .address(address)
                .build();
    }
}

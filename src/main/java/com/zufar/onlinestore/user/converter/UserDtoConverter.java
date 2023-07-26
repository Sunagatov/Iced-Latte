package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.user.dto.AddressDto;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.Address;
import com.zufar.onlinestore.user.entity.UserEntity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

import java.util.Set;

@Service
@AllArgsConstructor
public class UserDtoConverter {
    private final AddressDtoConverter addressDtoConverter;
    private final PasswordEncoder passwordEncoder;

    public UserDto toDto(final UserEntity entity) {
        AddressDto addressDto = addressDtoConverter.toDto(entity.getAddress());
        return new UserDto(
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUserName(),
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

    public User toUser(final UserDto dto) {
        SimpleGrantedAuthority userAuthority = new SimpleGrantedAuthority("User");
        final Set<GrantedAuthority> authorities = Set.of(userAuthority);
        return new User(
                dto.userName(),
                passwordEncoder.encode(dto.password()),
                authorities
        );
    }
}

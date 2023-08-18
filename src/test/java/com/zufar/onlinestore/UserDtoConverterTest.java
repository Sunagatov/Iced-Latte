package com.zufar.onlinestore;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class UserDtoConverterTest {

    @Autowired
    UserDtoConverter converter;
    private UserEntity user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = Instancio.create(UserEntity.class);
        userDto = Instancio.create(UserDto.class);
    }

    @Test
    void shouldCheckUserDoesNotEqualToNull() {
        assertNotNull(user);
    }

    @Test
    void shouldCheckUserDtoAfterMappingDoesNotEqualToNull() {
        UserDto dto = converter.toDto(user);

        assertNotNull(dto);
    }

    @Test
    void shouldMapUserToDto() {
        UserDto dto = converter.toDto(user);

        assertAll(
                () -> assertEquals(dto.userId(), user.getUserId()),
                () -> assertEquals(dto.firstName(), user.getFirstName()),
                () -> assertEquals(dto.lastName(), user.getLastName()),
                () -> assertEquals(dto.username(), user.getUsername()),
                () -> assertEquals(dto.email(), user.getEmail()),
                () -> assertEquals(dto.password(), user.getPassword()),
                () -> assertEquals(dto.password(), user.getPassword()),
                () -> assertEquals(dto.address().line(), user.getAddress().getLine()),
                () -> assertEquals(dto.address().city(), user.getAddress().getCity()),
                () -> assertEquals(dto.address().country(), user.getAddress().getCountry())
        );
    }

    @Test
    void shouldCheckUserDtoDoesNotEqualToNull() {
        assertNotNull(userDto);
    }

    @Test
    void shouldCheckUserAfterMappingDoesNotEqualToNull() {
        UserEntity entity = converter.toEntity(userDto);

        assertNotNull(entity);
    }

    @Test
    void shouldMapUserDtoToEntity() {
        UserEntity user = converter.toEntity(userDto);

        assertAll(
                () -> assertEquals(user.getUserId(), userDto.userId()),
                () -> assertEquals(user.getFirstName(), userDto.firstName()),
                () -> assertEquals(user.getLastName(), userDto.lastName()),
                () -> assertEquals(user.getUsername(), userDto.username()),
                () -> assertEquals(user.getEmail(), userDto.email()),
                () -> assertEquals(user.getPassword(), userDto.password()),
                () -> assertEquals(user.getPassword(), userDto.password()),
                () -> assertEquals(user.getAddress().getLine(), userDto.address().line()),
                () -> assertEquals(user.getAddress().getCity(), userDto.address().city()),
                () -> assertEquals(user.getAddress().getCountry(), userDto.address().country()),
                () -> assertEquals(user.getAuthorities().stream().findAny().get().getUser().getEmail(), user.getEmail())
        );
    }
}
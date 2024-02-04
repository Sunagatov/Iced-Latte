package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class UserDtoConverterTest {

    @InjectMocks
    private UserDtoConverter userDtoConverter;

    @Mock
    private AddressDtoConverter addressDtoConverter;

    @Test
    @DisplayName("toDto should convert UserEntity to UserDto with complete user information")
    void toDtoShouldConvertUserEntityToUserDtoWithCompleteUserInformation() {
        UserEntity entity = UserDtoTestStub.createUserEntity();
        UserDto dto = userDtoConverter.toDto(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getFirstName(), dto.getFirstName());
        assertEquals(entity.getLastName(), dto.getLastName());
        assertEquals(entity.getBirthDate(), dto.getBirthDate());
        assertEquals(entity.getPhoneNumber(), dto.getPhoneNumber());
        assertEquals(entity.getEmail(), dto.getEmail());
        assertEquals(entity.getStripeCustomerToken(), dto.getStripeCustomerToken());
    }
}
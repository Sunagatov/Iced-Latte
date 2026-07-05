package com.zufar.icedlatte.user.converter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserDtoConverterTest.Config.class)
class UserDtoConverterTest {

    @Configuration
    static class Config {
        @Bean
        UserDtoConverter userDtoConverter() {
            return Mappers.getMapper(UserDtoConverter.class);
        }

        @Bean
        AddressDtoConverter addressDtoConverter() {
            return Mappers.getMapper(AddressDtoConverter.class);
        }
    }

    @Autowired
    private UserDtoConverter userDtoConverter;

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
        assertNotNull(dto.getAddress());
        assertEquals(entity.getAddress().getLine(), dto.getAddress().getLine());
        assertEquals(entity.getAddress().getCity(), dto.getAddress().getCity());
        assertEquals(entity.getAddress().getCountry(), dto.getAddress().getCountry());
    }

    @Test
    @DisplayName("updateEntity should update existing address instead of replacing it")
    void updateEntityShouldUpdateExistingAddressInsteadOfReplacingIt() {
        Address existingAddress = Address.builder()
                .country("United Kingdom")
                .city("London")
                .line("221B Baker Street")
                .postcode("NW1 6XE")
                .build();
        UserEntity entity = UserEntity.builder()
                .firstName("Old")
                .lastName("Name")
                .address(existingAddress)
                .build();
        AddressDto newAddress = new AddressDto();
        newAddress.setCountry("France");
        newAddress.setCity("Paris");
        newAddress.setLine("10 Rue de Rivoli");
        newAddress.setPostcode("75001");
        UpdateUserAccountRequest request = new UpdateUserAccountRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setAddress(newAddress);

        userDtoConverter.updateEntity(entity, request);

        assertEquals(existingAddress, entity.getAddress());
        assertEquals("France", existingAddress.getCountry());
        assertEquals("Paris", existingAddress.getCity());
        assertEquals("10 Rue de Rivoli", existingAddress.getLine());
        assertEquals("75001", existingAddress.getPostcode());
    }

    @Test
    @DisplayName("updateEntity should preserve existing address when request omits address")
    void updateEntityShouldPreserveExistingAddressWhenAddressIsOmitted() {
        Address existingAddress = Address.builder()
                .country("United Kingdom")
                .city("London")
                .line("221B Baker Street")
                .postcode("NW1 6XE")
                .build();
        UserEntity entity = UserEntity.builder()
                .firstName("Old")
                .lastName("Name")
                .address(existingAddress)
                .build();
        UpdateUserAccountRequest request = new UpdateUserAccountRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setAddress(null);

        userDtoConverter.updateEntity(entity, request);

        assertEquals(existingAddress, entity.getAddress());
        assertEquals("United Kingdom", entity.getAddress().getCountry());
        assertEquals("London", entity.getAddress().getCity());
        assertEquals("221B Baker Street", entity.getAddress().getLine());
        assertEquals("NW1 6XE", entity.getAddress().getPostcode());
    }

    @Test
    @DisplayName("updateEntity should clear address when request sends an empty address object")
    void updateEntityShouldClearAddressWhenRequestSendsEmptyAddressObject() {
        UserEntity entity = UserEntity.builder()
                .firstName("Old")
                .lastName("Name")
                .address(Address.builder()
                        .country("United Kingdom")
                        .city("London")
                        .line("221B Baker Street")
                        .postcode("NW1 6XE")
                        .build())
                .build();
        UpdateUserAccountRequest request = new UpdateUserAccountRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setAddress(new AddressDto());

        userDtoConverter.updateEntity(entity, request);

        assertNull(entity.getAddress());
    }

    @Test
    @DisplayName("updateEntity should reject partial address updates")
    void updateEntityShouldRejectPartialAddressUpdates() {
        UserEntity entity =
                UserEntity.builder().firstName("Old").lastName("Name").build();
        AddressDto partialAddress = new AddressDto();
        partialAddress.setCountry("France");
        partialAddress.setCity("Paris");
        partialAddress.setLine("10 Rue de Rivoli");
        UpdateUserAccountRequest request = new UpdateUserAccountRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setAddress(partialAddress);

        assertThatThrownBy(() -> userDtoConverter.updateEntity(entity, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("address.postcode is required");
    }
}

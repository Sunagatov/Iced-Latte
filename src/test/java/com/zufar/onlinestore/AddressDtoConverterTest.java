package com.zufar.onlinestore;

import com.zufar.onlinestore.user.converter.AddressDtoConverter;
import com.zufar.onlinestore.user.dto.AddressDto;
import com.zufar.onlinestore.user.entity.Address;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AddressDtoConverterTest {

    @Autowired
    private AddressDtoConverter converter;
    private Address address;
    private AddressDto addressDto;

    @BeforeEach
    void setUp() {
        address = Instancio.create(Address.class);
        addressDto = Instancio.create(AddressDto.class);
    }

    @Test
    void shouldCheckAddressDoesNotEqualToNull() {
        assertNotNull(address);
    }

    @Test
    void shouldCheckAddressDtoAfterMappingDoesNotEqualToNull() {
        AddressDto dto = converter.toDto(address);

        assertNotNull(dto);
    }

    @Test
    void shouldMapAddressToDto() {
        AddressDto dto = converter.toDto(address);

        assertAll(
                () -> assertEquals(dto.line(), address.getLine()),
                () -> assertEquals(dto.city(), address.getCity()),
                () -> assertEquals(dto.country(), address.getCountry())
        );
    }

    @Test
    void shouldCheckAddressDtoDoesNotEqualToNull() {
        assertNotNull(addressDto);
    }

    @Test
    void shouldCheckAddressAfterMappingDoesNotEqualToNull() {
        Address entity = converter.toEntity(addressDto);

        assertNotNull(entity);
    }

    @Test
    void shouldMapAddressDtoToEntity() {
        Address entity = converter.toEntity(addressDto);

        assertAll(
                () -> assertEquals(entity.getLine(), addressDto.line()),
                () -> assertEquals(entity.getCity(), addressDto.city()),
                () -> assertEquals(entity.getCountry(), addressDto.country())
        );
    }
}
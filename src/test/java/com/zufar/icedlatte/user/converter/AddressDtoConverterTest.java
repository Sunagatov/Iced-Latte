package com.zufar.icedlatte.user.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.stub.AddressDtoTestStub;

class AddressDtoConverterTest {

    private final AddressDtoConverter converter = Mappers.getMapper(AddressDtoConverter.class);

    @Test
    @DisplayName("toDto should convert Address to AddressDto")
    void toDtoShouldConvertAddressToAddressDto() {
        Address address = AddressDtoTestStub.createAddressEntity();

        AddressDto dto = converter.toDto(address);

        assertEquals(address.getLine(), dto.getLine());
        assertEquals(address.getCity(), dto.getCity());
        assertEquals(address.getCountry(), dto.getCountry());
    }

    @Test
    @DisplayName("toEntity should convert AddressDto to Address")
    void toEntityShouldConvertAddressDtoToAddress() {
        AddressDto dto = AddressDtoTestStub.createAddressDto();

        Address address = Objects.requireNonNull(converter.toEntity(dto));

        assertEquals(dto.getLine(), address.getLine());
        assertEquals(dto.getCity(), address.getCity());
        assertEquals(dto.getCountry(), address.getCountry());
    }
}

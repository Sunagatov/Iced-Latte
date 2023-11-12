package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.openapi.dto.AddressDto;
import com.zufar.onlinestore.user.entity.Address;
import com.zufar.onlinestore.user.stub.AddressDtoTestStub;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        Address address = converter.toEntity(dto);

        assertEquals(dto.getLine(), address.getLine());
        assertEquals(dto.getCity(), address.getCity());
        assertEquals(dto.getCountry(), address.getCountry());
    }
}


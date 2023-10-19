package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.openapi.dto.AddressDto;
import com.zufar.onlinestore.user.entity.Address;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.DisplayName;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressDtoConverterTest {

    private final AddressDtoConverter converter = Mappers.getMapper(AddressDtoConverter.class);

    @Test
    @DisplayName("toDto should convert Address to AddressDto")
    public void toDtoShouldConvertAddressToAddressDto() {
        Address address = Address.builder()
                .addressId(UUID.randomUUID())
                .line("123 Main St")
                .city("Sample City")
                .country("Sample Country")
                .build();

        AddressDto dto = converter.toDto(address);

        assertEquals(address.getLine(), dto.getLine());
        assertEquals(address.getCity(), dto.getCity());
        assertEquals(address.getCountry(), dto.getCountry());
    }

    @Test
    @DisplayName("toEntity should convert AddressDto to Address")
    public void toEntityShouldConvertAddressDtoToAddress() {
        AddressDto dto = new AddressDto();
        dto.setLine("456 Elm St");
        dto.setCity("Test City");
        dto.setCountry("Test Country");

        Address address = converter.toEntity(dto);

        assertEquals(dto.getLine(), address.getLine());
        assertEquals(dto.getCity(), address.getCity());
        assertEquals(dto.getCountry(), address.getCountry());
    }
}


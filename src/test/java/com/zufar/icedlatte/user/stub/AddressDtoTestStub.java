package com.zufar.icedlatte.user.stub;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.entity.Address;

import java.util.UUID;

public class AddressDtoTestStub {

    public static Address createAddressEntity() {
        return Address.builder()
                .addressId(UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3"))
                .line("123 Main St")
                .city("Sample City")
                .country("Sample Country")
                .build();
    }

    public static AddressDto createAddressDto() {
        AddressDto addressDto = new AddressDto();
        addressDto.setLine("456 Elm St");
        addressDto.setCity("Test City");
        addressDto.setCountry("Test Country");
        return addressDto;
    }
}

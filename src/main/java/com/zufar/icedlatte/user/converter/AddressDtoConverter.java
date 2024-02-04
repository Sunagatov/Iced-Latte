package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.entity.Address;
import org.springframework.stereotype.Service;

@Service
public class AddressDtoConverter {

    public AddressDto toDto(final Address entity){
        return new AddressDto(
                entity.getCountry(),
                entity.getCity(),
                entity.getLine(),
                entity.getPostcode()
        );
    }

    public Address toEntity(final AddressDto dto){
        Address address = new Address();
        address.setCountry(dto.getCountry());
        address.setCity(dto.getCity());
        address.setLine(dto.getLine());
        address.setPostcode(dto.getPostcode());
        return address;
    }
}

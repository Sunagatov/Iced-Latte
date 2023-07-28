package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.user.dto.AddressDto;
import com.zufar.onlinestore.user.entity.Address;
import org.springframework.stereotype.Service;

@Service
public class AddressDtoConverter {

    public AddressDto toDto(final Address entity) {
        return new AddressDto(
                entity.getLine(),
                entity.getCity(),
                entity.getCountry()
        );
    }

    public Address toEntity(final AddressDto dto) {
        return Address.builder()
                .line(dto.line())
                .city(dto.city())
                .country(dto.country())
                .build();
    }
}

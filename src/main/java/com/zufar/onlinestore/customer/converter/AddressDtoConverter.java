package com.zufar.onlinestore.customer.converter;

import com.zufar.onlinestore.customer.dto.AddressDto;
import com.zufar.onlinestore.customer.entity.Address;

import org.springframework.stereotype.Service;

@Service
public class AddressDtoConverter {

	public AddressDto convertToDto(final Address entity) {
		return AddressDto.builder()
				.line(entity.getLine())
				.city(entity.getCity())
				.country(entity.getCountry())
				.build();
	}

	public Address convertToEntity(final AddressDto dto) {
		return Address.builder()
				.line(dto.getLine())
				.city(dto.getCity())
				.country(dto.getCountry())
				.build();
	}
}

package com.zufar.onlinestore.converter;

import com.zufar.onlinestore.dto.AddressDto;
import com.zufar.onlinestore.dto.CustomerDto;
import com.zufar.onlinestore.model.Address;
import com.zufar.onlinestore.model.Customer;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CustomerDtoConverter {
	private final AddressDtoConverter addressDtoConverter;

	public CustomerDto convertToDto(final Customer entity) {
		AddressDto addressDto = addressDtoConverter.convertToDto(entity.getAddress());
		return CustomerDto.builder()
				.customerId(entity.getCustomerId())
				.firstName(entity.getFirstName())
				.lastName(entity.getLastName())
				.email(entity.getEmail())
				.address(addressDto)
				.build();
	}

	public Customer convertToEntity(final CustomerDto dto) {
		Address addressDto = addressDtoConverter.convertToEntity(dto.getAddress());
		return Customer.builder()
				.customerId(dto.getCustomerId())
				.firstName(dto.getFirstName())
				.lastName(dto.getLastName())
				.email(dto.getEmail())
				.address(addressDto)
				.build();
	}
}

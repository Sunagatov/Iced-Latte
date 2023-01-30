package com.zufar.onlinestore.customer.converter;

import com.zufar.onlinestore.customer.dto.AddressDto;
import com.zufar.onlinestore.customer.dto.CustomerDto;
import com.zufar.onlinestore.customer.entity.Address;
import com.zufar.onlinestore.customer.entity.Customer;

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

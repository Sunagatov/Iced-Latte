package com.zufar.onlinestore.converter;

import com.zufar.onlinestore.dto.ProductInfoDto;
import com.zufar.onlinestore.model.ProductInfo;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductInfoDtoConverter {
	private final AddressDtoConverter addressDtoConverter;

	public ProductInfoDto convertToDto(final ProductInfo entity) {
		return ProductInfoDto.builder()
				.category(entity.getCategory())
				.name(entity.getName())
				.price(entity.getPrice())
				.build();
	}

	public ProductInfo convertToEntity(final ProductInfoDto dto) {
		return ProductInfo.builder()
				.category(dto.getCategory())
				.name(dto.getName())
				.price(dto.getPrice())
				.build();
	}
}

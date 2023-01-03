package com.zufar.onlinestore.service;

import com.zufar.onlinestore.dto.PriceDto;
import com.zufar.onlinestore.dto.ProductInfoDto;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

@Service
public class ProductsSumCalculator {

	public BigDecimal calculate(final Collection<ProductInfoDto> products) {
		return products.stream()
				.map(ProductInfoDto::getPrice)
				.map(PriceDto::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}

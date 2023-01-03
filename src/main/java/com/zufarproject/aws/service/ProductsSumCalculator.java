package com.zufarproject.aws.service;

import com.zufarproject.aws.dto.PriceDto;
import com.zufarproject.aws.dto.ProductInfoDto;

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

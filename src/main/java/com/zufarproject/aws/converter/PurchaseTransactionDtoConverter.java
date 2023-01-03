package com.zufarproject.aws.converter;

import com.zufarproject.aws.dto.PurchaseProductsRequest;
import com.zufarproject.aws.dto.PurchaseTransactionDto;
import com.zufarproject.aws.service.ProductsSumCalculator;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseTransactionDtoConverter {
	private final ProductsSumCalculator productsSumCalculator;

	public PurchaseTransactionDto convert(final PurchaseProductsRequest request) {
		BigDecimal totalSum = productsSumCalculator.calculate(request.getProducts());

		return PurchaseTransactionDto.builder()
				.transactionId(UUID.randomUUID())
				.customerId(request.getCustomerId())
				.totalSum(totalSum)
				.createdAt(LocalDateTime.now())
				.build();
	}
}

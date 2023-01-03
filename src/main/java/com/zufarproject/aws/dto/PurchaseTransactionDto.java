package com.zufarproject.aws.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseTransactionDto {

	private UUID transactionId;
	private String customerId;
	private BigDecimal totalSum;
	private LocalDateTime createdAt;
}

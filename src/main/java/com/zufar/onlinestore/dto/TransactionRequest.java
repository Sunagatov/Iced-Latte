package com.zufar.onlinestore.dto;


import com.zufar.onlinestore.product.dto.ProductInfoDto;

import java.util.Collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {

	@NotBlank
	private String customerId;

	@NotNull
	private Collection<ProductInfoDto> products;
}

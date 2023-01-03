package com.zufarproject.aws.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceDto {

	@NotNull(message = "Price is mandatory")
	@DecimalMin(value = "10.0", message = "Price minimum value should be more than 10")
	private BigDecimal amount;

	@NotBlank(message = "Currency is mandatory")
	@Size(max = 55, message = "Currency length must be less than 55 characters")
	private String currency;
}

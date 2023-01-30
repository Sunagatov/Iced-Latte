package com.zufar.onlinestore.dto;

import jakarta.validation.Valid;
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
public class ProductInfoDto {

	private int id;

	@NotBlank(message = "Name is mandatory")
	@Size(max = 55, message = "Name length must be less than 55 characters")
	private String name;

	@Valid
	@NotNull(message = "Price is mandatory")
	private PriceDto price;

	@NotBlank(message = "Category is mandatory")
	@Size(max = 55, message = "Category length must be less than 55 characters")
	private String category;
}

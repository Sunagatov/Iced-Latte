package com.zufar.onlinestore.model;

import com.zufar.onlinestore.dto.PriceDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
public class ProductInfo {

	@Id
	private int id;

	private String name;
	private PriceDto price;
	private String category;
}

package com.zufar.onlinestore.product.entity;

import com.zufar.onlinestore.product.dto.PriceDto;
import com.zufar.onlinestore.review.entity.Review;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Document
public class ProductInfo {

	@Id
	private int id;

	private String name;
	private PriceDto price;
	private String category;
	@DBRef
	private List<Review> reviews = new ArrayList<>();
}

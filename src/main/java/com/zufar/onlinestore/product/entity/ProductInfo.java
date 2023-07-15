package com.zufar.onlinestore.product.entity;

import com.zufar.onlinestore.product.dto.PriceDto;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Builder
@Document
public class ProductInfo {

	@Id
	private int id;

	private String name;

	private PriceDto price;

	private String category;

	public ProductInfo(int id, String name, PriceDto price, String category) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.category = category;
	}

	public ProductInfo() {
	}

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public PriceDto getPrice() {
		return this.price;
	}

	public String getCategory() {
		return this.category;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrice(PriceDto price) {
		this.price = price;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof final ProductInfo other)) return false;
		if (!other.canEqual(this)) return false;
		if (this.getId() != other.getId()) return false;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (!Objects.equals(this$name, other$name)) return false;
		final Object this$price = this.getPrice();
		final Object other$price = other.getPrice();
		if (!Objects.equals(this$price, other$price)) return false;
		final Object this$category = this.getCategory();
		final Object other$category = other.getCategory();
		return Objects.equals(this$category, other$category);
	}

	protected boolean canEqual(final Object other) {
		return other instanceof ProductInfo;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + this.getId();
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $price = this.getPrice();
		result = result * PRIME + ($price == null ? 43 : $price.hashCode());
		final Object $category = this.getCategory();
		result = result * PRIME + ($category == null ? 43 : $category.hashCode());
		return result;
	}

	public String toString() {
		return "ProductInfo(id=" + this.getId() + ", name=" + this.getName() + ", price=" + this.getPrice() + ", category=" + this.getCategory() + ")";
	}
}

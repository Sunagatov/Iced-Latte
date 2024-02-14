package com.zufar.icedlatte.filter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class ProductFilterDto {
    private BigDecimal maxPrice;
    private BigDecimal minPrice;
    private String name;
}

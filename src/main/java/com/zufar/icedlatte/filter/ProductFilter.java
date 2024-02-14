package com.zufar.icedlatte.filter;

import com.zufar.icedlatte.filter.dto.ProductFilterDto;
import com.zufar.icedlatte.product.entity.ProductInfo;

import java.util.List;
import java.util.stream.Stream;

public interface ProductFilter {
    boolean isApplicable(ProductFilterDto productFilterDto);

    List<ProductInfo> apply(Stream<ProductInfo> productStream, ProductFilterDto productFilterDto);
}

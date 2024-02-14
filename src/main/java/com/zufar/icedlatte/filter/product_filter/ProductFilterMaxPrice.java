package com.zufar.icedlatte.filter.product_filter;

import com.zufar.icedlatte.filter.ProductFilter;
import com.zufar.icedlatte.filter.dto.ProductFilterDto;
import com.zufar.icedlatte.product.entity.ProductInfo;

import java.util.List;
import java.util.stream.Stream;

public class ProductFilterMaxPrice implements ProductFilter {

    @Override
    public boolean isApplicable(ProductFilterDto productFilterDto) {
        return productFilterDto.getMaxPrice() != null;
    }

        @Override
        public List<ProductInfo> apply(Stream<ProductInfo> productStream, ProductFilterDto productFilterDto) {
            return productStream.filter(
                    product -> product.getPrice().compareTo(productFilterDto.getMaxPrice()) <= 0
            ).toList();
        }
}

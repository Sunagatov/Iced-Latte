package com.zufar.icedlatte.filter.api;

import com.zufar.icedlatte.filter.ProductFilter;
import com.zufar.icedlatte.filter.dto.ProductFilterDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProductFilterList {
    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final List<ProductFilter>productFilters;

    public List<ProductInfoDto> getProductFilterList(ProductFilterDto productFilterDto){
        List<ProductInfo> productInfo = productInfoRepository
                .findAll();
        return applyFilter(productInfo.stream(), productFilterDto);
    }

    private List<ProductInfoDto> applyFilter(Stream<ProductInfo> productInfoStream, ProductFilterDto productFilterDto) {
        List<ProductInfo> productInfo = new ArrayList<>();
        List<ProductFilter> requiredFilters = productFilters.stream()
                .filter(filter -> filter.isApplicable(productFilterDto))
                .toList();
        for (ProductFilter filter : requiredFilters) {
            productInfo = filter.apply(productInfoStream, productFilterDto);
        }
        return productInfo.stream().map(productInfoDtoConverter::toDto).toList();
    }
}

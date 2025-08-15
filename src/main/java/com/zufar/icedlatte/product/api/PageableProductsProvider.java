package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageableProductsProvider {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductUpdater productUpdater;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ProductListWithPaginationInfoDto getProducts(final Pageable pageable,
                                                        final BigDecimal minPrice,
                                                        final BigDecimal maxPrice,
                                                        final Integer minimumAverageRating,
                                                        final List<String> brandNames,
                                                        final List<String> sellerNames) {
        BigDecimal minAvg = (minimumAverageRating == null) ? null : BigDecimal.valueOf(minimumAverageRating);
        List<String> brands = nullIfEmpty(brandNames);
        List<String> sellers = nullIfEmpty(sellerNames);

        Page<ProductInfoDto> productsWithPageInfo = productInfoRepository
                .findAllProducts(minPrice, maxPrice, minAvg, brands, sellers, pageable)
                .map(productInfoDtoConverter::toDto)
                .map(productUpdater::update);

        return productInfoDtoConverter.toProductPaginationDto(productsWithPageInfo);
    }

    private static <T> List<T> nullIfEmpty(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }
}

package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.zufar.icedlatte.common.util.Utils.createPageableObject;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageableProductsProvider {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductUpdater productUpdater;
    private final PaginationConfig paginationConfig;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ProductListWithPaginationInfoDto getProducts(final Integer pageNumber,
                                                        final Integer pageSize,
                                                        final String sortAttribute,
                                                        final String sortDirection,
                                                        final BigDecimal minPrice,
                                                        final BigDecimal maxPrice,
                                                        final Integer minimumAverageRating,
                                                        final List<String> brandNames,
                                                        final List<String> sellerNames) {
        int page = pageNumber != null ? pageNumber : paginationConfig.getDefaultPageNumber();
        int size = pageSize != null ? pageSize : paginationConfig.getProducts().getDefaultPageSize();
        String sortAttr = sortAttribute != null ? sortAttribute : paginationConfig.getProducts().getDefaultSortAttribute();
        String sortDir = sortDirection != null ? sortDirection : paginationConfig.getProducts().getDefaultSortDirection();

        log.info("Getting products: page={}, size={}, sort_attribute={}, sort_direction={}", page, size, sortAttr, sortDir);

        BigDecimal minAvg = minimumAverageRating == null ? null : BigDecimal.valueOf(minimumAverageRating);

        Page<ProductInfoDto> result = productInfoRepository
                .findAllProducts(minPrice, maxPrice, minAvg, nullIfEmpty(brandNames), nullIfEmpty(sellerNames),
                        createPageableObject(page, size, sortAttr, sortDir))
                .map(productInfoDtoConverter::toDto)
                .map(productUpdater::update);

        return productInfoDtoConverter.toProductPaginationDto(result);
    }

    private static <T> List<T> nullIfEmpty(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }
}

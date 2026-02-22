package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.zufar.icedlatte.common.util.Utils.createPageableObject;
import static com.zufar.icedlatte.product.repository.ProductSpecifications.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageableProductsProvider {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;
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

        log.info("product.list.fetching: page={}, size={}, sort_attribute={}, sort_direction={}", page, size, sortAttr, sortDir);
        long t0 = System.currentTimeMillis();

        BigDecimal minAvg = minimumAverageRating == null ? null : BigDecimal.valueOf(minimumAverageRating);

        Specification<ProductInfo> spec = Specification.allOf(
                minPriceSpec(minPrice),
                maxPriceSpec(maxPrice),
                minRatingSpec(minAvg),
                brandNamesSpec(brandNames),
                sellerNamesSpec(sellerNames)
        );

        Page<ProductInfoDto> result = productInfoRepository
                .findAll(spec, createPageableObject(page, size, sortAttr, sortDir))
                .map(productInfoDtoConverter::toDto)
                .map(productPictureLinkUpdater::update);

        log.info("product.list.fetched: count={}, durationMs={}", result.getNumberOfElements(), System.currentTimeMillis() - t0);
        return productInfoDtoConverter.toProductPaginationDto(result);
    }
}

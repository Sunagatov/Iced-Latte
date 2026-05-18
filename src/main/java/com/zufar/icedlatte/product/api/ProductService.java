package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.pagination.PageRequestFactory;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.product.validator.GetProductsRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zufar.icedlatte.product.repository.ProductSpecifications.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;
    private final PaginationConfig paginationConfig;
    private final GetProductsRequestValidator getProductsRequestValidator;

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, isolation = Isolation.READ_COMMITTED)
    @Cacheable(cacheNames = "productById", key = "#productId")
    public ProductInfoDto getProductById(final UUID productId) {
        var product = productInfoRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return productPictureLinkUpdater.update(productInfoDtoConverter.toDto(product));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<ProductInfoDto> getProductsByIds(final List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ProductInfoDto> products = productInfoRepository.findAllById(ids).stream()
                .map(productInfoDtoConverter::toDto)
                .toList();
        List<ProductInfoDto> productsWithImages = productPictureLinkUpdater.updateBatch(products);

        var productsById = productsWithImages.stream()
                .collect(Collectors.toMap(ProductInfoDto::getId, Function.identity()));
        List<UUID> missing = ids.stream().filter(id -> !productsById.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ProductNotFoundException(missing);
        }
        return ids.stream().map(productsById::get).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ProductListWithPaginationInfoDto getProducts(final Integer pageNumber,
                                                        final Integer pageSize,
                                                        final String sortAttribute,
                                                        final String sortDirection,
                                                        final BigDecimal minPrice,
                                                        final BigDecimal maxPrice,
                                                        final Integer minimumAverageRating,
                                                        final List<String> brandNames,
                                                        final List<String> sellerNames,
                                                        final String keyword) {
        getProductsRequestValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection,
                minPrice, maxPrice, minimumAverageRating, brandNames, sellerNames);

        int page = pageNumber != null ? pageNumber : paginationConfig.getDefaultPageNumber();
        int size = pageSize != null ? pageSize : paginationConfig.getProducts().getDefaultPageSize();
        String sortAttr = sortAttribute != null ? sortAttribute : paginationConfig.getProducts().getDefaultSortAttribute();
        String sortDir = sortDirection != null ? sortDirection : paginationConfig.getProducts().getDefaultSortDirection();

        BigDecimal minAvg = minimumAverageRating == null ? null : BigDecimal.valueOf(minimumAverageRating);

        Specification<ProductInfo> spec = Specification.allOf(
                minPriceSpec(minPrice),
                maxPriceSpec(maxPrice),
                minRatingSpec(minAvg),
                brandNamesSpec(brandNames),
                sellerNamesSpec(sellerNames),
                nameContainsSpec(keyword)
        );

        Page<ProductInfo> rawPage = productInfoRepository
                .findAll(spec, PageRequestFactory.of(page, size, sortAttr, sortDir));

        List<ProductInfoDto> products = rawPage.getContent().stream()
                .map(productInfoDtoConverter::toDto)
                .toList();
        List<ProductInfoDto> productsWithImages = productPictureLinkUpdater.updateBatch(products);

        Page<ProductInfoDto> result = new PageImpl<>(
                productsWithImages, rawPage.getPageable(), rawPage.getTotalElements());

        return productInfoDtoConverter.toProductPaginationDto(result);
    }

    @Cacheable(cacheNames = "sellers")
    @Transactional(readOnly = true)
    public List<String> getSellerNames() {
        return productInfoRepository.findDistinctSellerNames();
    }

    @Cacheable(cacheNames = "brands")
    @Transactional(readOnly = true)
    public List<String> getBrandNames() {
        return productInfoRepository.findDistinctBrandNames();
    }
}

package com.zufar.icedlatte.product.service;

import static com.zufar.icedlatte.product.specification.ProductSpecifications.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.pagination.PageRequestFactory;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService implements ProductCatalogApi {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;
    private final PaginationConfig paginationConfig;
    private final GetProductsRequestValidator getProductsRequestValidator;

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, isolation = Isolation.READ_COMMITTED)
    @Cacheable(cacheNames = "productById", key = "#productId")
    public ProductInfoDto getProductDtoById(final UUID productId) {
        var product =
                productInfoRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        return productPictureLinkUpdater.update(productInfoDtoConverter.toDto(product));
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<ProductSnapshot> getProductsByIds(final List<UUID> ids) {
        return getProductDtosByIds(ids).stream()
                .map(productInfoDtoConverter::toSnapshot)
                .toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<ProductInfoDto> getProductDtosByIds(final List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        validateProductIds(ids);
        List<ProductInfoDto> products = productInfoRepository.findAllById(ids).stream()
                .map(productInfoDtoConverter::toDto)
                .toList();
        List<ProductInfoDto> productsWithImages = productPictureLinkUpdater.updateBatch(products);

        var productsById =
                productsWithImages.stream().collect(Collectors.toMap(ProductInfoDto::getId, Function.identity()));
        List<UUID> missing =
                ids.stream().filter(id -> !productsById.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ProductNotFoundException(missing);
        }
        return ids.stream().map(productsById::get).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ProductListWithPaginationInfoDto getProductDtos(
            final @Nullable Integer pageNumber,
            final @Nullable Integer pageSize,
            final @Nullable String sortAttribute,
            final @Nullable String sortDirection,
            final @Nullable BigDecimal minPrice,
            final @Nullable BigDecimal maxPrice,
            final @Nullable Integer minimumAverageRating,
            final @Nullable List<String> brandNames,
            final @Nullable List<String> sellerNames,
            final @Nullable String keyword) {
        getProductsRequestValidator.validate(
                pageNumber,
                pageSize,
                sortAttribute,
                sortDirection,
                minPrice,
                maxPrice,
                minimumAverageRating,
                brandNames,
                sellerNames,
                keyword);

        int page = pageNumber != null ? pageNumber : paginationConfig.defaultPageNumber();
        int size = pageSize != null ? pageSize : paginationConfig.products().defaultPageSize();
        String sortAttr = sortAttribute != null
                ? sortAttribute
                : paginationConfig.products().defaultSortAttribute();
        String sortDir = sortDirection != null
                ? sortDirection
                : paginationConfig.products().defaultSortDirection();

        BigDecimal minAvg = minimumAverageRating == null ? null : BigDecimal.valueOf(minimumAverageRating);

        Specification<ProductInfo> spec = Specification.allOf(
                minPriceSpec(minPrice),
                maxPriceSpec(maxPrice),
                minRatingSpec(minAvg),
                brandNamesSpec(brandNames),
                sellerNamesSpec(sellerNames),
                nameContainsSpec(keyword));

        Page<ProductInfo> rawPage =
                productInfoRepository.findAll(spec, PageRequestFactory.of(page, size, sortAttr, sortDir));

        List<ProductInfoDto> products = rawPage.getContent().stream()
                .map(productInfoDtoConverter::toDto)
                .toList();
        List<ProductInfoDto> productsWithImages = productPictureLinkUpdater.updateBatch(products);

        Page<ProductInfoDto> result =
                new PageImpl<>(productsWithImages, rawPage.getPageable(), rawPage.getTotalElements());

        return productInfoDtoConverter.toProductPaginationDto(result);
    }

    @Override
    @Cacheable(cacheNames = "sellers")
    @Transactional(readOnly = true)
    public List<String> getSellerNames() {
        return productInfoRepository.findDistinctSellerNames();
    }

    @Override
    @Cacheable(cacheNames = "brands")
    @Transactional(readOnly = true)
    public List<String> getBrandNames() {
        return productInfoRepository.findDistinctBrandNames();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(final UUID productId) {
        return productInfoRepository.existsById(productId);
    }

    private static void validateProductIds(List<@Nullable UUID> ids) {
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("Product ids must not contain null values.");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BadRequestException("Product ids must not contain duplicate values.");
        }
    }
}

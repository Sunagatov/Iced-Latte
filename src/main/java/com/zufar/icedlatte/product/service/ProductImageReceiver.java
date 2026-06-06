package com.zufar.icedlatte.product.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.filestorage.api.FileUrlResolverApi;
import com.zufar.icedlatte.product.config.ProductCacheConfigurationProvider;
import com.zufar.icedlatte.product.entity.ProductImage;
import com.zufar.icedlatte.product.repository.ProductImageRepository;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageReceiver {

    @Getter
    @Value("${product.placeholder-image-url}")
    private String placeholderImageUrl;

    private final FileUrlResolverApi fileUrlResolverApi;
    private final ProductImageRepository productImageRepository;
    private final MeterRegistry meterRegistry;

    @Cacheable(
            cacheNames = ProductCacheConfigurationProvider.PRODUCT_IMAGE_URL,
            key = "#productId",
            unless = "#result == @productImageReceiver.getPlaceholderImageUrl()")
    public String getProductFileUrl(final UUID productId) {
        try {
            return fileUrlResolverApi.findFileUrl(productId).orElseGet(() -> {
                log.debug("product.image.not_found: productId={}", productId);
                return placeholderImageUrl;
            });
        } catch (RuntimeException ex) {
            meterRegistry.counter("product.image.fallback", "mode", "single").increment();
            log.error(
                    "product.image.error: productId={}, exceptionClass={}",
                    productId,
                    ex.getClass().getSimpleName(),
                    ex);
            return placeholderImageUrl;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ProductCacheConfigurationProvider.PRODUCT_IMAGE_URLS, key = "#productId")
    public List<String> getProductImageUrls(final UUID productId) {
        return productImageRepository.findByProductIdOrderByPositionAscIdAsc(productId).stream()
                .map(ProductImage::getUrl)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<String>> getProductImageUrlsBatch(final List<UUID> productIds) {
        return productImageRepository.findByProductIdInOrderByProductIdAscPositionAscIdAsc(productIds).stream()
                .collect(Collectors.groupingBy(
                        ProductImage::getProductId, Collectors.mapping(ProductImage::getUrl, Collectors.toList())));
    }

    public Map<UUID, String> getProductFileUrls(final List<UUID> productIds) {
        Map<UUID, String> fileUrls;
        try {
            fileUrls = fileUrlResolverApi.findFileUrls(productIds);
        } catch (RuntimeException ex) {
            meterRegistry.counter("product.image.fallback", "mode", "batch").increment();
            log.error(
                    "product.images.error: count={}, exceptionClass={}",
                    productIds.size(),
                    ex.getClass().getSimpleName(),
                    ex);
            fileUrls = Map.of();
        }
        final Map<UUID, String> resolved = fileUrls;
        return productIds.stream()
                .collect(Collectors.toMap(id -> id, id -> resolved.getOrDefault(id, placeholderImageUrl)));
    }
}

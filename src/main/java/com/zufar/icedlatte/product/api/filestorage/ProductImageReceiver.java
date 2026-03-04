package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import com.zufar.icedlatte.product.entity.ProductImage;
import com.zufar.icedlatte.product.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageReceiver {

    private static final String DEFAULT_FILE_URL = "/assets/images/product-placeholder.png";

    private final FileProvider fileProvider;
    private final ProductImageRepository productImageRepository;

    @Cacheable(cacheNames = "productImageUrl", key = "#productId", unless = "#result.startsWith('/assets/')")
    public String getProductFileUrl(final UUID productId) {
        try {
            return fileProvider.getRelatedObjectUrl(productId)
                    .orElseGet(() -> {
                        log.warn("product.image.not_found: productId={}", productId);
                        return DEFAULT_FILE_URL;
                    });
        } catch (RuntimeException ex) {
            log.error("product.image.error: productId={}, message={}", productId, ex.getMessage(), ex);
            return DEFAULT_FILE_URL;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productImageUrls", key = "#productId")
    public List<String> getProductImageUrls(final UUID productId) {
        return productImageRepository.findByProductIdOrderByPosition(productId)
                .stream()
                .map(ProductImage::getUrl)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<String>> getProductImageUrlsBatch(final List<UUID> productIds) {
        return productImageRepository.findByProductIdInOrderByPosition(productIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ProductImage::getProductId,
                        Collectors.mapping(ProductImage::getUrl, Collectors.toList())
                ));
    }

    public Map<UUID, String> getProductFileUrls(final List<UUID> productIds) {
        Map<UUID, String> fileUrls;
        try {
            fileUrls = fileProvider.getRelatedObjectUrls(productIds);
        } catch (RuntimeException ex) {
            log.error("product.images.error: count={}, message={}", productIds.size(), ex.getMessage(), ex);
            fileUrls = Map.of();
        }
        final Map<UUID, String> resolved = fileUrls;
        return productIds.stream()
                .collect(Collectors.toMap(id -> id, id -> resolved.getOrDefault(id, DEFAULT_FILE_URL)));
    }
}

package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SingleProductProvider {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductUpdater productUpdater;

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, isolation = Isolation.READ_COMMITTED)
    @Cacheable(cacheNames = "productById", key = "#productId")
    public ProductInfoDto getProductById(final UUID productId) {
        return productInfoRepository.findById(productId)
                .map(productInfoDtoConverter::toDto)
                .map(productUpdater::update)
                .orElseThrow(() -> {
                    log.error("The product with id = {} was not found.", productId);
                    return new ProductNotFoundException(productId);
                });
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ProductInfo getProductEntityById(final UUID productId) {
        return productInfoRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Failed to get the product entity: {}", productId);
                    return new ProductNotFoundException(productId);
                });
    }
}

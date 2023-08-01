package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.exception.ProductNotFoundException;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductInfoDto getProductById(final UUID productId) {
        return productInfoRepository.findById(productId)
                .map(productInfoDtoConverter::toDto)
                .orElseThrow(() -> {
                    log.error("The product with id = {} is not found.", productId);
                    throw new ProductNotFoundException(productId);
                });
    }
}
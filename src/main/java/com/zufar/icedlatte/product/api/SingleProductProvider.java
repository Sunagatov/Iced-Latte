package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.converter.ProductInfoDtoMapStractConverter;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
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
    private final ProductInfoDtoMapStractConverter productInfoDtoMapStractConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductInfoDto getProductById(final UUID productId) {
        return productInfoRepository.findById(productId)
                .map(productInfoDtoMapStractConverter::toDto)
                .orElseThrow(() -> {
                    log.error("The product with id = {} is not found.", productId);
                    return new ProductNotFoundException(productId);
                });
    }
}

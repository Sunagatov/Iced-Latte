package com.zufar.onlinestore.product.service.impl;

import com.zufar.onlinestore.product.mapper.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductResponseDto;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.product.service.ProductApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductInfoServiceImpl implements ProductApi {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;

    @Override
    public Page<ProductResponseDto> getProducts(Pageable pageable) {
        log.info("Received request to get all Products (service)");
        return productInfoRepository.findAll(pageable)
                .map(productInfoDtoConverter::toResponseDto);
    }
}
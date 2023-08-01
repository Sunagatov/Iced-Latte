package com.zufar.onlinestore.product.service;

import com.zufar.onlinestore.product.dto.ProductResponseDto;
import com.zufar.onlinestore.product.mapper.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class GetProduct {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;

    public ProductResponseDto getProduct(UUID id) {
        log.info("Received request to get Product (service)");
        return productInfoDtoConverter.toResponseDto(productInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Course with id=%s not found", id)
                )));
    }
}
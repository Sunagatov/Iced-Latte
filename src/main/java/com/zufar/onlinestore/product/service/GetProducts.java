package com.zufar.onlinestore.product.service;

import com.zufar.onlinestore.product.dto.ProductPaginationDto;
import com.zufar.onlinestore.product.dto.ProductResponseDto;
import com.zufar.onlinestore.product.mapper.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class GetProducts {
    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;

    public ProductPaginationDto getProducts(Integer page,
                                            Integer size,
                                            String sortAttribute,
                                            String sortDirection) {
        log.info("Received request to get all Products (service)");
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(sortDirection), sortAttribute));
        Page<ProductResponseDto> pageProductResponseDto = productInfoRepository.findAll(pageable)
                .map(productInfoDtoConverter::toResponseDto);
        return addToProductPaginationDto(pageProductResponseDto);
    }

    private ProductPaginationDto addToProductPaginationDto(Page<ProductResponseDto> pageProductResponseDto) {
        return new ProductPaginationDto(
                pageProductResponseDto.getContent(),
                pageProductResponseDto.getNumber(),
                pageProductResponseDto.getSize(),
                pageProductResponseDto.getTotalElements(),
                pageProductResponseDto.getTotalPages()
        );
    }
}
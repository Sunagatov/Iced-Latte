package com.zufar.onlinestore.product.endpoint;

import com.zufar.onlinestore.product.mapper.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductResponseDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;

import com.zufar.onlinestore.product.service.ProductApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/products")
public class ProductsEndpoint {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductApi productInfoService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductInfoDto> getProductInfoById(@PathVariable("id") final String id) {
        log.info("Received request to get the ProductInfo with id - {}.", id);
        Optional<ProductInfo> ProductInfo = productInfoRepository.findById(UUID.fromString(id));
        if (ProductInfo.isEmpty()) {
            log.info("the ProductInfo with id - {} is absent.", id);
            return ResponseEntity.notFound()
                    .build();
        }
        ProductInfoDto ProductInfoDto = productInfoDtoConverter.toDto(ProductInfo.get());
        log.info("the ProductInfo with id - {} was retrieved - {}.", id, ProductInfoDto);
        return ResponseEntity.ok()
                .body(ProductInfoDto);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(Pageable pageable) {
        log.info("Received request to get all ProductInfos (controller): {}", pageable);
        Page<ProductResponseDto> productInfoCollection = productInfoService.getProducts(pageable);
        if (productInfoCollection.isEmpty()) {
            log.info("All ProductInfos are absent.");
            return ResponseEntity.notFound()
                    .build();
        }
        log.info("All ProductInfos were retrieved - {}.", productInfoCollection);
        return ResponseEntity.ok()
                .body(productInfoCollection);
    }

}
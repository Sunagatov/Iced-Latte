package com.zufar.onlinestore.product.endpoint;

import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
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

    @GetMapping("/{id}")
    public ResponseEntity<ProductInfoDto> getProductInfoById(@PathVariable("id") final UUID id) {
        log.info("Received request to get the ProductInfo with id - {}.", id);
        Optional<ProductInfo> ProductInfo = productInfoRepository.findById(id);
        if (ProductInfo.isEmpty()) {
            log.info("the ProductInfo with id - {} is absent.", id);
            return ResponseEntity.notFound()
                    .build();
        }
        ProductInfoDto ProductInfoDto = productInfoDtoConverter.convertToDto(ProductInfo.get());
        log.info("the ProductInfo with id - {} was retrieved - {}.", id, ProductInfoDto);
        return ResponseEntity.ok()
                .body(ProductInfoDto);
    }

    @GetMapping
    public ResponseEntity<Collection<ProductInfoDto>> getAllProducts() {
        log.info("Received request to get all ProductInfos");
        Collection<ProductInfo> productInfoCollection = productInfoRepository.findAll();
        if (productInfoCollection.isEmpty()) {
            log.info("All ProductInfos are absent.");
            return ResponseEntity.notFound()
                    .build();
        }
        Collection<ProductInfoDto> ProductInfos = productInfoCollection.stream()
                .map(productInfoDtoConverter::convertToDto)
                .toList();

        log.info("All ProductInfos were retrieved - {}.", ProductInfos);
        return ResponseEntity.ok()
                .body(ProductInfos);
    }

}

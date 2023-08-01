package com.zufar.onlinestore.product.endpoint;

import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.product.dto.ProductPaginationDto;
import com.zufar.onlinestore.product.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/v1/products")
public class ProductsEndpoint {

    private final ProductApi productInfoService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable final String productId) {
        log.info("Received request to get the product with productId - {}.", productId);
        UUID convertedId = UUID.fromString(productId);
        ProductResponseDto product = productInfoService.getProduct(convertedId);
        return ResponseEntity.ok()
                .body(product);
    }

    @GetMapping
    public ResponseEntity<ProductPaginationDto> getAllProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestParam(name = "sort_attribute", defaultValue = "name") String sortAttribute,
            @RequestParam(name = "sort_direction", defaultValue = "desc") String sortDirection) {
        log.info("Received request to get all products (controller): " +
                        "page - {}, size - {}, sort_attribute - {}, sort_direction - {}",
                page, size, sortAttribute, sortDirection);
        ProductPaginationDto productPaginationDto = productInfoService.getAllProducts(
                page,
                size,
                sortAttribute,
                sortDirection);
        if (productPaginationDto.products().isEmpty()) {
            log.info("All ProductInfos are absent.");
            return ResponseEntity.notFound()
                    .build();
        }
        log.info("All Products were retrieved - {}.", productPaginationDto);
        return ResponseEntity.ok()
                .body(productPaginationDto);
    }

}
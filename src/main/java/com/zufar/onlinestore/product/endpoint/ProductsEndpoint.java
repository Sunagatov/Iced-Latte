package com.zufar.onlinestore.product.endpoint;

import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.openapi.dto.ProductInfoDto;
import com.zufar.onlinestore.openapi.dto.ProductListWithPaginationInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = ProductsEndpoint.PRODUCTS_URL)
public class ProductsEndpoint implements com.zufar.onlinestore.openapi.product.api.ProductApi {

    public static final String PRODUCTS_URL = "/api/v1/products";

    private final ProductApi productApi;

    @CrossOrigin(origins = "http://localhost:3000")
    @Override
    @GetMapping("/{productId}")
    public ResponseEntity<ProductInfoDto> getProductById(@PathVariable final String productId) {
        log.info("Received the request to get the product with productId - {}.", productId);
        ProductInfoDto product = productApi.getProduct(UUID.fromString(productId));
        log.info("The product with productId: {} was retrieved successfully", productId);
        return ResponseEntity.ok()
                .body(product);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @Override
    @GetMapping
    public ResponseEntity<ProductListWithPaginationInfoDto> getProducts(@RequestParam(name = "page", defaultValue = "0") Integer page,
                                                                        @RequestParam(name = "size", defaultValue = "50") Integer size,
                                                                        @RequestParam(name = "sort_attribute", defaultValue = "name") String sortAttribute,
                                                                        @RequestParam(name = "sort_direction", defaultValue = "desc") String sortDirection) {
        log.info("Received the request to get products with these pagination and sorting attributes: page - {}, size - {}, sort_attribute - {}, sort_direction - {}",
                page, size, sortAttribute, sortDirection);
        ProductListWithPaginationInfoDto productPaginationDto = productApi.getProducts(page, size, sortAttribute, sortDirection);
        log.info("Products were retrieved successfully with these pagination and sorting attributes: page - {}, size - {}, sort_attribute - {}, sort_direction - {}",
                page, size, sortAttribute, sortDirection);
        return ResponseEntity.ok()
                .body(productPaginationDto);
    }
}
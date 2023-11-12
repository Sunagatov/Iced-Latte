package com.zufar.onlinestore.product.endpoint;

import com.zufar.onlinestore.openapi.dto.ProductInfoDto;
import com.zufar.onlinestore.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.onlinestore.product.api.ProductApi;
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
@RequestMapping(value = ProductsEndpoint.PRODUCTS_URL)
public class ProductsEndpoint implements com.zufar.onlinestore.openapi.product.api.ProductApi {

    public static final String PRODUCTS_URL = "/api/v1/products";

    public static final String GET_SINGLE_PRODUCT_PATH = "/{productId}";

    private final ProductApi productApi;

    @Override
    @GetMapping(GET_SINGLE_PRODUCT_PATH)
    public ResponseEntity<ProductInfoDto> getProductById(@PathVariable final UUID productId) {
        log.info("Received the request to get the product with productId - {}.", productId);
        ProductInfoDto product = productApi.getProduct(productId);
        log.info("The product with productId: {} was retrieved successfully", productId);
        return ResponseEntity.ok()
                .body(product);
    }

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
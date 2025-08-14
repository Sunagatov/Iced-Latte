package com.zufar.icedlatte.product.endpoint;

import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.product.api.*;
import com.zufar.icedlatte.product.validator.GetProductsRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


import static com.zufar.icedlatte.common.util.Utils.createPageableObject;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = ProductsEndpoint.PRODUCTS_URL)
public class ProductsEndpoint implements com.zufar.icedlatte.openapi.product.api.ProductApi {

    public static final String PRODUCTS_URL = "/api/v1/products";

    private final ProductsProvider productsProvider;
    private final PageableProductsProvider pageableProductsProvider;
    private final GetProductsRequestValidator getProductsRequestValidator;
    private final SingleProductProvider singleProductProvider;

    @Override
    @GetMapping("/{productId}")
    public ResponseEntity<ProductInfoDto> getProductById(@PathVariable final UUID productId) {
        // Validate UUID input to prevent code injection
        if (productId == null) {
            log.warn("Invalid product ID: null value received");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Getting product by id: {}", productId);
        var product = singleProductProvider.getProductById(productId);
        log.info("Product retrieved: {}", productId);
        return ResponseEntity.ok(product);
    }

    @Override
    @GetMapping
    @Validated
    public ResponseEntity<ProductListWithPaginationInfoDto> getProducts(@RequestParam(name = "page", defaultValue = "0") Integer pageNumber,
                                                                        @RequestParam(name = "size", defaultValue = "50") Integer pageSize,
                                                                        @RequestParam(name = "sort_attribute", defaultValue = "name") String sortAttribute,
                                                                        @RequestParam(name = "sort_direction", defaultValue = "desc") String sortDirection,
                                                                        @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
                                                                        @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
                                                                        @RequestParam(name = "minimum_average_rating", required = false) Integer minimumAverageRating,
                                                                        @RequestParam(name = "brand_names", required = false) List<String> brandNames,
                                                                        @RequestParam(name = "seller_names", required = false) List<String> sellersNames) {
        log.info("Getting products with pagination: page={}, size={}, sort={}", pageNumber, pageSize, sortAttribute);
        getProductsRequestValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection, minPrice, maxPrice, minimumAverageRating, brandNames, sellersNames);
        var pageable = createPageableObject(pageNumber, pageSize, sortAttribute, sortDirection);
        var products = pageableProductsProvider.getProducts(pageable, minPrice, maxPrice, minimumAverageRating, brandNames, sellersNames);
        log.info("Retrieved {} products", products.getProducts().size());
        return ResponseEntity.ok(products);
    }

    @Override
    @PostMapping("/ids")
    public ResponseEntity<List<ProductInfoDto>> getProductsByIds(@Valid @RequestBody final ProductIdsDto productIdsDto) {
        // Validate input to prevent code injection
        if (productIdsDto.getProductIds() == null || productIdsDto.getProductIds().isEmpty()) {
            log.warn("Invalid request: null or empty product IDs list");
            return ResponseEntity.badRequest().build();
        }
        
        var productIds = productIdsDto.getProductIds();
        log.info("Getting {} products by IDs", productIds.size());
        var products = productsProvider.getProducts(productIds);
        log.info("Retrieved {} products by IDs", products.size());
        return ResponseEntity.ok(products);
    }

    @Override
    @GetMapping("/sellers")
    public ResponseEntity<SellersDto> getAllSellers() {
        log.info("Getting all sellers");
        var sellers = new SellersDto(List.of("JavaBeanCoffee", "FreshCup", "BrewedBliss", "EspressoEmporium", "MorningMug", "CoffeeCorner", "CuppaCafe", "BeanBrewers"));
        log.info("Retrieved {} sellers", sellers.getSellers().size());
        return ResponseEntity.ok(sellers);
    }

    @Override
    @GetMapping("/brands")
    public ResponseEntity<BrandsDto> getAllBrands() {
        log.info("Getting all brands");
        var brands = new BrandsDto(List.of("Folgers", "Illy", "Dunkin-Donuts", "Nescafe", "Lavazza", "Peets-Coffee", "Starbucks"));
        log.info("Retrieved {} brands", brands.getBrands().size());
        return ResponseEntity.ok(brands);
    }
}
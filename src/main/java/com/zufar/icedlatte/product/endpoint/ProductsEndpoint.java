package com.zufar.icedlatte.product.endpoint;

import com.zufar.icedlatte.common.config.PaginationConfig;
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
@RequestMapping(ProductsEndpoint.PRODUCTS_URL)
public class ProductsEndpoint implements com.zufar.icedlatte.openapi.product.api.ProductApi {

    public static final String PRODUCTS_URL = "/api/v1/products";

    private final ProductsProvider productsProvider;
    private final PageableProductsProvider pageableProductsProvider;
    private final GetProductsRequestValidator getProductsRequestValidator;
    private final SingleProductProvider singleProductProvider;
    private final PaginationConfig paginationConfig;

    @Override
    @GetMapping("/{productId}")
    public ResponseEntity<ProductInfoDto> getProductById(@PathVariable final UUID productId) {
        log.info("Getting product by id: {}", productId);
        var product = singleProductProvider.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    @Override
    @GetMapping
    public ResponseEntity<ProductListWithPaginationInfoDto> getProducts(
            @RequestParam(name = "page", required = false) Integer pageNumber,
            @RequestParam(name = "size", required = false) Integer pageSize,
            @RequestParam(name = "sort_attribute", required = false) String sortAttribute,
            @RequestParam(name = "sort_direction", required = false) String sortDirection,
            @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @RequestParam(name = "minimum_average_rating", required = false) Integer minimumAverageRating,
            @RequestParam(name = "brand_names", required = false) List<String> brandNames,
            @RequestParam(name = "seller_names", required = false) List<String> sellerNames) {

        // Apply default values from configuration
        pageNumber = pageNumber != null ? pageNumber : paginationConfig.getDefaultPageNumber();
        pageSize = pageSize != null ? pageSize : paginationConfig.getProducts().getDefaultPageSize();
        sortAttribute = sortAttribute != null ? sortAttribute : paginationConfig.getProducts().getDefaultSortAttribute();
        sortDirection = sortDirection != null ? sortDirection : paginationConfig.getProducts().getDefaultSortDirection();

        log.info("Getting products: page={}, size={}, sort={} {}", pageNumber, pageSize, sortAttribute, sortDirection);
        getProductsRequestValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection,
                minPrice, maxPrice, minimumAverageRating, brandNames, sellerNames);

        Pageable pageable = createPageableObject(pageNumber, pageSize, sortAttribute, sortDirection);
        var products = pageableProductsProvider.getProducts(pageable, minPrice, maxPrice,
                minimumAverageRating, brandNames, sellerNames);

        return ResponseEntity.ok(products);
    }

    @Override
    @PostMapping("/ids")
    public ResponseEntity<List<ProductInfoDto>> getProductsByIds(@Valid @RequestBody final ProductIdsDto productIdsDto) {
        var ids = productIdsDto.getProductIds();
        if (ids == null || ids.isEmpty()) {
            log.warn("Empty products ids payload");
            return ResponseEntity.badRequest().build();
        }
        log.info("Getting {} products by IDs", ids.size());
        var products = productsProvider.getProducts(ids);
        log.info("Retrieved {} products by IDs", products.size());
        return ResponseEntity.ok(products);
    }

    @Override
    @GetMapping("/sellers")
    public ResponseEntity<SellersDto> getAllSellers() {
        log.info("Getting all sellers");
        var sellers = new SellersDto(List.of(
                "JavaBeanCoffee", "FreshCup", "BrewedBliss", "EspressoEmporium",
                "MorningMug", "CoffeeCorner", "CuppaCafe", "BeanBrewers"
        ));
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
package com.zufar.icedlatte.product.endpoint;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.BrandsDto;
import com.zufar.icedlatte.openapi.dto.ProductIdsDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.openapi.dto.SellersDto;
import com.zufar.icedlatte.product.api.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(ProductsEndpoint.PRODUCTS_URL)
public class ProductsEndpoint implements com.zufar.icedlatte.openapi.product.api.ProductApi {

    public static final String PRODUCTS_URL = ApiPaths.PRODUCTS;

    private final ProductService productService;

    @Override
    @GetMapping("/sellers")
    public ResponseEntity<SellersDto> getAllSellers() {
        return ResponseEntity.ok(new SellersDto(productService.getSellerNames()));
    }

    @Override
    @GetMapping("/brands")
    public ResponseEntity<BrandsDto> getAllBrands() {
        return ResponseEntity.ok(new BrandsDto(productService.getBrandNames()));
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
            @RequestParam(name = "seller_names", required = false) List<String> sellerNames,
            @RequestParam(name = "keyword", required = false) String keyword) {

        return ResponseEntity.ok(productService.getProducts(
                pageNumber, pageSize, sortAttribute, sortDirection,
                minPrice, maxPrice, minimumAverageRating, brandNames, sellerNames, keyword));
    }

    @Override
    @PostMapping("/ids")
    public ResponseEntity<List<ProductInfoDto>> getProductsByIds(@Valid @RequestBody final ProductIdsDto productIdsDto) {
        var products = productService.getProductsByIds(productIdsDto.getProductIds());
        log.debug("product.ids.fetched: count={}", products.size());
        return ResponseEntity.ok(products);
    }

    @Override
    @GetMapping("/{productId}")
    public ResponseEntity<ProductInfoDto> getProductById(@PathVariable final UUID productId) {
        log.debug("product.get: productId={}", productId);
        return ResponseEntity.ok(productService.getProductById(productId));
    }
}

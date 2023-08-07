package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductApiImpl implements ProductApi {

    private final PageableProductsProvider pageableProductsProvider;
    private final SingleProductProvider singleProductProvider;

    @Override
    public ProductListWithPaginationInfoDto getProducts(final Integer page,
                                                        final Integer size,
                                                        final String sortAttribute,
                                                        final String sortDirection) {
        log.info("The product service a get products method is called");
        return pageableProductsProvider.getProducts(page, size, sortAttribute, sortDirection);
    }

    @Override
    public ProductInfoDto getProduct(final UUID productId) {
        log.info("The product service a get single product method is called");
        return singleProductProvider.getProductById(productId);
    }
}
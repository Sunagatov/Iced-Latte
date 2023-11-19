package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
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
        return pageableProductsProvider.getProducts(page, size, sortAttribute, sortDirection);
    }

    @Override
    public ProductInfoDto getProduct(final UUID productId) {
        return singleProductProvider.getProductById(productId);
    }
}
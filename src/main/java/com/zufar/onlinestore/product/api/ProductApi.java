package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.openapi.dto.ProductInfoDto;
import com.zufar.onlinestore.openapi.dto.ProductListWithPaginationInfoDto;

import java.util.UUID;

public interface ProductApi {

    /**
     * Enables to get the product list with pagination and sorting features
     *
     * @param page          page number in order (starting from 0)
     * @param size          number of products in the output
     * @param sortAttribute the name of the product field by which they should be sorted
     * @param sortDirection sorting direction (DESC or ASC)
     * @return ProductPaginationDto the sorted list of products includes:
     * page number, number of products per page, total number of products, total number of pages.
     */
    ProductListWithPaginationInfoDto getProducts(final Integer page,
                                                 final Integer size,
                                                 final String sortAttribute,
                                                 final String sortDirection);

    /**
     * Enables to get a product by its id.
     *
     * @param productId the identifier of the product which is returned as the return value
     * @return ProductInfoDto the product details
     */
    ProductInfoDto getProduct(final UUID productId);
}
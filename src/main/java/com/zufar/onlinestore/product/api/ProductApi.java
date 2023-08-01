package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.product.dto.ProductPaginationDto;
import com.zufar.onlinestore.product.dto.ProductResponseDto;

import java.util.UUID;

public interface ProductApi {

    /**
     * Enables to get a list of all products sorted with the ability to change the output parameters.
     *
     * @param page page number in order (starting from 0)
     * @param size number of products in the output
     * @param sortAttribute the name of the product field by which they should be sorted
     * @param sortDirection sorting direction (DESC or ASC)
     * @return ProductPaginationDto the sorted list of products includes:
     * page number, number of products per page, total number of products, total number of pages.
     */
    ProductPaginationDto getAllProducts(Integer page,
                                        Integer size,
                                        String sortAttribute,
                                        String sortDirection);

    /**
     * Enables to get a product by its id.
     *
     * @param id product id.
     * @return ProductResponseDto a product with the required fields.
     */
    ProductResponseDto getProduct(UUID id);
}
package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;

import java.util.UUID;

public interface ProductApi {

    /**
     * Enables to get the product list with pagination and sorting features
     *
     * @param page is the number of pages
     * @param size is the products' quantity which are returned in a response object
     * @param sortAttribute is the attribute which is used for pages sorting
     * @param sortDirection is the direction of the pages sorting
     * @return ProductPaginationDto
     */
    ProductListWithPaginationInfoDto getProducts(Integer page,
                                                 Integer size,
                                                 String sortAttribute,
                                                 String sortDirection);

    /**
     * Enables to get ProductInfo
     *
     * @param productId the identifier of the product which is returned as the return value
     * @return ProductInfoDto the product details
     */
    ProductInfoDto getProduct(UUID productId);
}
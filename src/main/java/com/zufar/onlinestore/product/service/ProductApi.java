package com.zufar.onlinestore.product.service;

import com.zufar.onlinestore.product.dto.ProductPaginationDto;

public interface ProductApi {

    ProductPaginationDto getProducts(Integer page,
                                     Integer size,
                                     String sortAttribute,
                                     String sortDirection);
}
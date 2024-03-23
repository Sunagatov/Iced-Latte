package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.api.rating.ProductAverageRatingUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductUpdater {

    private final ProductPictureLinkUpdater productPictureLinkUpdater;
    private final ProductAverageRatingUpdater productAverageRatingUpdater;

    public ProductInfoDto update(ProductInfoDto productInfoDto) {
        productPictureLinkUpdater.update(productInfoDto);
        productAverageRatingUpdater.update(productInfoDto);
        return productInfoDto;
    }
}

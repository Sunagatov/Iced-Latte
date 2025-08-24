package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductUpdater {

    private final ProductPictureLinkUpdater productPictureLinkUpdater;

    public ProductInfoDto update(ProductInfoDto productInfoDto) {
        productPictureLinkUpdater.update(productInfoDto);
        return productInfoDto;
    }

    public List<ProductInfoDto> updateBatch(List<ProductInfoDto> productInfoDtos) {
        return productPictureLinkUpdater.updateBatch(productInfoDtos);
    }
}

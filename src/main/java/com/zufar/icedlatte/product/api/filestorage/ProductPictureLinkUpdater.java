package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPictureLinkUpdater {

    private final ProductImageReceiver productImageReceiver;

    public ProductInfoDto update(ProductInfoDto productInfoDto) {
        final UUID productId = productInfoDto.getId();
        final String productFileUrl = productImageReceiver.getProductFileUrl(productId);
        productInfoDto.setProductFileUrl(productFileUrl);
        return productInfoDto;
    }
}

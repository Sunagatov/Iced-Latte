package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import org.springframework.stereotype.Component;

@Component
public class ProductInfoDtoConverter extends ProductInfoLinkUpdater{
    public ProductInfoDtoConverter(ProductInfoDtoMapStractConverter productInfoDtoMapStractConverter, MinioTemporaryLinkReceiver minioTemporaryLinkReceiver) {
        super(productInfoDtoMapStractConverter, minioTemporaryLinkReceiver);
    }
}

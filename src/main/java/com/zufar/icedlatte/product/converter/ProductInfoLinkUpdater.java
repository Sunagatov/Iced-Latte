package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductInfoLinkUpdater {

    private final ProductInfoDtoMapStractConverter productInfoDtoMapStractConverter;
    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;

    public ProductInfoDto toProductInfoDto(final ProductInfo productInfo) {
        final String bucketName = productInfo.getFileMetadata().getBucketName();
        final String fileName = productInfo.getFileMetadata().getFileName();
        ProductInfoDto productInfoDto = productInfoDtoMapStractConverter.toDto(productInfo);
        final String temporaryLink = minioTemporaryLinkReceiver.generatePresignedUrl(bucketName, fileName).toString();
        productInfoDto.setProductFile(temporaryLink);
        return productInfoDto;
    }
}

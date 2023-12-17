package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.common.entity.FileMetadata;
import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductInfoLinkUpdater {

    private final ProductInfoDtoMapStractConverter productInfoDtoMapStractConverter;
    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;

    public ProductInfoDto toDto(final ProductInfo productInfo) {
        final FileMetadata fileMetadata = productInfo.getFileMetadata();
        if (fileMetadata == null) {
            return productInfoDtoMapStractConverter.toDto(productInfo);
        }
        final String bucketName = fileMetadata.getBucketName();
        final String fileName = fileMetadata.getFileName();
        ProductInfoDto productInfoDto = productInfoDtoMapStractConverter.toDto(productInfo);
        final String temporaryLink = minioTemporaryLinkReceiver.generatePresignedUrl(bucketName, fileName).toString();
        productInfoDto.setProductFile(temporaryLink);
        return productInfoDto;
    }

    public ProductListWithPaginationInfoDto toProductPaginationDto(final Page<ProductInfoDto> pageProductResponseDto) {
        return productInfoDtoMapStractConverter.toProductPaginationDto(pageProductResponseDto);
    }
}

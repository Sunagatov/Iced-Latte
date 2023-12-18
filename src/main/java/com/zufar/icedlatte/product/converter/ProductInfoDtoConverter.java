package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.common.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.common.dto.FileMetadataDto;
import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductInfoDtoConverter {

    private final ProductInfoDtoMapstructConverter productInfoDtoMapstructConverter;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;
    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;

    @Named("toProductInfoDto")
    public ProductInfoDto toDto(final ProductInfo productInfo) {
        ProductInfoDto productInfoDto = productInfoDtoMapstructConverter.toDto(productInfo);
        final FileMetadataDto fileMetadata = fileMetadataDtoConverter.toDto(productInfo.getFileMetadata());
        if (fileMetadata != null) {
            final String temporaryLink = minioTemporaryLinkReceiver.generatePresignedUrlAsString(fileMetadata);
            productInfoDto.setProductFileUrl(temporaryLink);
        }
        return productInfoDto;
    }

    public ProductListWithPaginationInfoDto toProductPaginationDto(final Page<ProductInfoDto> pageProductResponseDto) {
        return productInfoDtoMapstructConverter.toProductPaginationDto(pageProductResponseDto);
    }
}

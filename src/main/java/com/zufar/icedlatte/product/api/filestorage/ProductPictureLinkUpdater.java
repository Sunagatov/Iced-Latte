package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import com.zufar.icedlatte.filestorage.minio.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPictureLinkUpdater {

    private final FileMetadataProvider fileMetadataProvider;
    private final ProductImageReceiver productImageReceiver;
    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;

    private Map<UUID, List<FileMetadataDto>> fileMetadataMap = new HashMap<>();

    public ProductInfoDto updateByProductId(final ProductInfoDto productInfoDto) {
        final UUID productId = productInfoDto.getId();
        final String productFileUrl = productImageReceiver.getProductFileUrl(productId);
        productInfoDto.setProductFileUrl(productFileUrl);
        return productInfoDto;
    }

    public List<ProductInfoDto> updateProductsFileUrl(final List<ProductInfoDto> productInfoDtos, final List<UUID> uuids) {
        prepareFileMetadataMap(uuids);
        return productInfoDtos.stream()
                .map(productInfoDto -> {
                    final UUID productId = productInfoDto.getId();
                    final FileMetadataDto fileMetadataDto = fileMetadataMap.get(productId).get(0);
                    return updateByFileMetadata(productInfoDto, fileMetadataDto);
                }).toList();
    }

    private ProductInfoDto updateByFileMetadata(final ProductInfoDto productInfoDto, final FileMetadataDto fileMetadataDto) {
        final String productFileUrl = minioTemporaryLinkReceiver.generatePresignedUrlAsString(fileMetadataDto);
        productInfoDto.setProductFileUrl(productFileUrl);
        return productInfoDto;
    }

    private void prepareFileMetadataMap(List<UUID> uuids) {
        List<FileMetadataDto> fileMetadataDtos = fileMetadataProvider.getAllFileMetadataDto(uuids);

        fileMetadataMap = fileMetadataDtos.stream()
                .collect(Collectors.groupingBy(FileMetadataDto::relatedObjectId));
    }
}

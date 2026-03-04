package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPictureLinkUpdater {

    private final ProductImageReceiver productImageReceiver;

    public ProductInfoDto update(ProductInfoDto productInfoDto) {
        UUID id = productInfoDto.getId();
        productInfoDto.setProductFileUrl(productImageReceiver.getProductFileUrl(id));
        productInfoDto.setProductImageUrls(productImageReceiver.getProductImageUrls(id));
        return productInfoDto;
    }

    public List<ProductInfoDto> updateBatch(List<ProductInfoDto> productInfoDtos) {
        List<UUID> productIds = productInfoDtos.stream()
                .map(ProductInfoDto::getId)
                .toList();

        Map<UUID, String> fileUrls = productImageReceiver.getProductFileUrls(productIds);
        Map<UUID, List<String>> imageUrls = productImageReceiver.getProductImageUrlsBatch(productIds);

        productInfoDtos.forEach(product -> {
            product.setProductFileUrl(fileUrls.get(product.getId()));
            product.setProductImageUrls(imageUrls.getOrDefault(product.getId(), List.of()));
        });

        return productInfoDtos;
    }
}

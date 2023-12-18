package com.zufar.icedlatte.product.api;


import com.zufar.icedlatte.common.filestorage.api.FileProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageReceiver {

    private final FileProvider fileProvider;

    public String getProductFileUrl(final UUID productId) {
        String productFileUrl = fileProvider.getRelatedObjectUrl(productId);
        return productFileUrl == null ? "default file" : productFileUrl;
    }
}

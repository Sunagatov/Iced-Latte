package com.zufar.icedlatte.product.api;


import com.zufar.icedlatte.common.filestorage.api.FileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageReceiver {

    private final FileProvider fileProvider;

    public String getProductFileUrl(final UUID productId) {
        String productFileUrl = null;
        try {
            productFileUrl = fileProvider.getRelatedObjectUrl(productId);
        } catch (Throwable exception) {
            log.error("FileProvider error", exception);
        }
        return productFileUrl == null ? "default file" : productFileUrl;
    }
}

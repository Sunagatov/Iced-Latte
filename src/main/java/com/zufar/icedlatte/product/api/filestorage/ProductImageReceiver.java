package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageReceiver {

    private static final String DEFAULT_FILE_URL = "/assets/images/product-placeholder.png";

    private final FileProvider fileProvider;

    public String getProductFileUrl(final UUID productId) {
        try {
            return fileProvider.getRelatedObjectUrl(productId)
                    .orElseGet(() -> {
                        log.warn("File with id = {} was not found.", productId);
                        return DEFAULT_FILE_URL;
                    });
        } catch (RuntimeException ex) {
            log.error("FileProvider error while resolving image for product {}", productId, ex);
            return DEFAULT_FILE_URL;
        }
    }
}

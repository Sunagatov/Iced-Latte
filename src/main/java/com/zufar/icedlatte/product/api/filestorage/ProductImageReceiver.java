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

    private final FileProvider fileProvider;

    public String getProductFileUrl(final UUID productId) {
        try {
            return fileProvider.getRelatedObjectUrl(productId)
                    .orElseGet(() -> {
                        log.warn("File with id = {} was not found.", productId);
                        return "default file";
                    });
        } catch (Throwable exception) {
            log.error("FileProvider error", exception);
        }
        return "default file";
    }
}

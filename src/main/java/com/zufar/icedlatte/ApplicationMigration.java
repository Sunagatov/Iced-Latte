package com.zufar.icedlatte;

import com.zufar.icedlatte.product.api.filestorage.ProductImageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationMigration implements ApplicationRunner {

    private final ProductImageUploader productImageUploader;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        productImageUploader.uploadProductImages();
    }
}

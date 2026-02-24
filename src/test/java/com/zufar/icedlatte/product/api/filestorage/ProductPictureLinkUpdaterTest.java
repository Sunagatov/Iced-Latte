package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductPictureLinkUpdater unit tests")
class ProductPictureLinkUpdaterTest {

    @Mock private ProductImageReceiver productImageReceiver;
    @InjectMocks private ProductPictureLinkUpdater updater;

    @Test
    @DisplayName("update sets productFileUrl from receiver")
    void update_setsUrl() {
        UUID id = UUID.randomUUID();
        ProductInfoDto dto = new ProductInfoDto();
        dto.setId(id);
        when(productImageReceiver.getProductFileUrl(id)).thenReturn("https://cdn.example.com/img.jpg");

        ProductInfoDto result = updater.update(dto);

        assertThat(result.getProductFileUrl()).isEqualTo("https://cdn.example.com/img.jpg");
    }

    @Test
    @DisplayName("updateBatch sets urls for all products from batch receiver")
    void updateBatch_setsAllUrls() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ProductInfoDto dto1 = new ProductInfoDto();
        dto1.setId(id1);
        ProductInfoDto dto2 = new ProductInfoDto();
        dto2.setId(id2);
        when(productImageReceiver.getProductFileUrls(List.of(id1, id2)))
                .thenReturn(Map.of(id1, "url1", id2, "url2"));

        List<ProductInfoDto> result = updater.updateBatch(List.of(dto1, dto2));

        assertThat(result.getFirst().getProductFileUrl()).isEqualTo("url1");
        assertThat(result.get(1).getProductFileUrl()).isEqualTo("url2");
    }

    @Test
    @DisplayName("updateBatch sets null url when id not in receiver result")
    void updateBatch_missingId_setsNull() {
        UUID id = UUID.randomUUID();
        ProductInfoDto dto = new ProductInfoDto();
        dto.setId(id);
        when(productImageReceiver.getProductFileUrls(List.of(id))).thenReturn(Map.of());

        List<ProductInfoDto> result = updater.updateBatch(List.of(dto));

        assertThat(result.getFirst().getProductFileUrl()).isNull();
    }
}

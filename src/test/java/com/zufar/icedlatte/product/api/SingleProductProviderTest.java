package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SingleProductProviderTest {

    @Mock
    private ProductInfoRepository productRepository;

    @Mock
    private ProductInfoDtoConverter productInfoConverter;

    @Mock
    private ProductUpdater productUpdater;


    @InjectMocks
    private SingleProductProvider productProvider;

    @Test
    void shouldReturnProductWhenProductIdExists() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(any(UUID.class))).thenReturn(Optional.of(mock(ProductInfo.class)));
        when(productInfoConverter.toDto(any(ProductInfo.class))).thenReturn(mock(ProductInfoDto.class));
        when(productUpdater.update(any(ProductInfoDto.class))).thenReturn(mock(ProductInfoDto.class));

        ProductInfoDto result = productProvider.getProductById(productId);

        assertNotNull(result);

        verify(productInfoConverter, times(1)).toDto(any(ProductInfo.class));
    }

    @Test
    void shouldThrowExceptionWhenProductIdNotExists() {
        UUID productId = UUID.randomUUID();

        when(productUpdater.update(any(ProductInfoDto.class))).thenReturn(any(ProductInfoDto.class));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        ProductNotFoundException thrownException = assertThrows(
                ProductNotFoundException.class,
                () -> productProvider.getProductById(productId)
        );

        assertEquals(
                String.format("The product with productId = %s is not found.", productId),
                thrownException.getMessage()
        );

        verify(productRepository, times(1)).findById(productId);
    }
}

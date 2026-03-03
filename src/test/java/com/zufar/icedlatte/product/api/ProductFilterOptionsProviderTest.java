package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductFilterOptionsProvider unit tests")
class ProductFilterOptionsProviderTest {

    @Mock
    private ProductInfoRepository productInfoRepository;
    @InjectMocks
    private ProductFilterOptionsProvider provider;

    @Test
    @DisplayName("getSellerNames delegates to repository")
    void getSellerNames_returnsList() {
        when(productInfoRepository.findDistinctSellerNames()).thenReturn(List.of("SellerA", "SellerB"));
        assertThat(provider.getSellerNames()).containsExactly("SellerA", "SellerB");
    }

    @Test
    @DisplayName("getBrandNames delegates to repository")
    void getBrandNames_returnsList() {
        when(productInfoRepository.findDistinctBrandNames()).thenReturn(List.of("BrandX"));
        assertThat(provider.getBrandNames()).containsExactly("BrandX");
    }

    @Test
    @DisplayName("getSellerNames returns empty list when none exist")
    void getSellerNames_empty_returnsEmpty() {
        when(productInfoRepository.findDistinctSellerNames()).thenReturn(List.of());
        assertThat(provider.getSellerNames()).isEmpty();
    }
}

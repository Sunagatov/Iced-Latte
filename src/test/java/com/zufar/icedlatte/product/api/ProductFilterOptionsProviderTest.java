package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductFilterOptionsProvider unit tests")
class ProductFilterOptionsProviderTest {

    @Mock private ProductInfoRepository productInfoRepository;
    @InjectMocks private ProductFilterOptionsProvider provider;

    @Nested
    @DisplayName("getSellerNames")
    class GetSellerNames {

        @Test
        @DisplayName("returns the repository values unchanged")
        void returnsRepositoryValuesUnchanged() {
            List<String> sellerNames = List.of("SellerA", "SellerB");
            when(productInfoRepository.findDistinctSellerNames()).thenReturn(sellerNames);

            List<String> result = provider.getSellerNames();

            assertThat(result).containsExactlyElementsOf(sellerNames);
            verify(productInfoRepository).findDistinctSellerNames();
            verifyNoMoreInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("returns an empty list when the repository has no sellers")
        void returnsEmptyListWhenRepositoryHasNoSellers() {
            when(productInfoRepository.findDistinctSellerNames()).thenReturn(List.of());

            assertThat(provider.getSellerNames()).isEmpty();
            verify(productInfoRepository).findDistinctSellerNames();
            verifyNoMoreInteractions(productInfoRepository);
        }
    }

    @Nested
    @DisplayName("getBrandNames")
    class GetBrandNames {

        @Test
        @DisplayName("returns the repository values unchanged")
        void returnsRepositoryValuesUnchanged() {
            List<String> brandNames = List.of("BrandX", "BrandY");
            when(productInfoRepository.findDistinctBrandNames()).thenReturn(brandNames);

            List<String> result = provider.getBrandNames();

            assertThat(result).containsExactlyElementsOf(brandNames);
            verify(productInfoRepository).findDistinctBrandNames();
            verifyNoMoreInteractions(productInfoRepository);
        }

        @Test
        @DisplayName("returns an empty list when the repository has no brands")
        void returnsEmptyListWhenRepositoryHasNoBrands() {
            when(productInfoRepository.findDistinctBrandNames()).thenReturn(List.of());

            assertThat(provider.getBrandNames()).isEmpty();
            verify(productInfoRepository).findDistinctBrandNames();
            verifyNoMoreInteractions(productInfoRepository);
        }
    }
}

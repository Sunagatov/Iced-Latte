package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;

import static com.zufar.onlinestore.product.util.ProductPaginationDefaults.PAGE;
import static com.zufar.onlinestore.product.util.ProductPaginationDefaults.SIZE;
import static com.zufar.onlinestore.product.util.ProductPaginationDefaults.SORT_ATTRIBUTE;
import static com.zufar.onlinestore.product.util.ProductPaginationDefaults.SORT_DIRECTION;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class PageableProductsProviderTest {

    @Mock
    private ProductInfoRepository productRepository;

    @Mock
    private ProductInfoDtoConverter productInfoConverter;

    @InjectMocks
    private PageableProductsProvider productsProvider;

    private List<ProductInfo> products;

    @BeforeEach
    void setUp() {
        products = Instancio.ofList(ProductInfo.class).create();
    }

    @Test
    void shouldFetchProductsUsingPageAttributes() {
        Page<ProductInfo> page = new PageImpl<>(products);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(productInfoConverter.toDto(any(ProductInfo.class))).thenReturn(mock(ProductInfoDto.class));
        when(productInfoConverter.toProductPaginationDto(ArgumentMatchers.<Page<ProductInfoDto>>any())).thenReturn(mock(ProductListWithPaginationInfoDto.class));

        ProductListWithPaginationInfoDto productList = productsProvider.getProducts(PAGE.getIntValue(), SIZE.getIntValue(),
                SORT_ATTRIBUTE.getStringValue(), SORT_DIRECTION.getStringValue()
        );

        assertNotNull(productList);

        verify(productRepository, times(1)).findAll(any(Pageable.class));
        verify(productInfoConverter, times(1)).toProductPaginationDto(ArgumentMatchers.<Page<ProductInfoDto>>any());
    }
}

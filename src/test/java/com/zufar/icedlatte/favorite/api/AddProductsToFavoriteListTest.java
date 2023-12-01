package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItem;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
public class AddProductsToFavoriteListTest {

    @InjectMocks
    private AddProductsToFavoriteList addProductsToFavoriteList;

    @Mock
    private GetFavoriteList getFavoriteList;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private FavoriteListDtoConverter favoriteListDtoConverter;

    private ListOfFavoriteProducts listOfFavoriteProducts = new ListOfFavoriteProducts();


    @Test
    @DisplayName("Should add products to favorite list returning favorite list")
    void shouldAddProductsToFavoriteListReturningFavoriteList() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductInfo productInfo = new ProductInfo();
        productInfo.setProductId(productId);

        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(productId);

        FavoriteItem favoriteItem = new FavoriteItem();
        favoriteItem.setProductInfo(productInfo);

        FavoriteItemDto favoriteItemDto = new FavoriteItemDto(
                UUID.randomUUID(),
                productInfoDto);

        FavoriteList favoriteList = new FavoriteList();
        favoriteList.setFavoriteItems(new HashSet<>());

        FavoriteList addedFavoriteList = new FavoriteList();
        addedFavoriteList.setFavoriteItems(Set.of(favoriteItem));

        FavoriteListDto expectedFavoriteListDto = new FavoriteListDto(UUID.randomUUID(),
                userId,
                Set.of(favoriteItemDto),
                OffsetDateTime.now());

        listOfFavoriteProducts.setProductIds(List.of(productId));

        when(getFavoriteList.getEntityFavoriteList(userId)).thenReturn(favoriteList);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(productInfo));
        when(favoriteRepository.save(favoriteList)).thenReturn(addedFavoriteList);
        when(favoriteListDtoConverter.toDto(favoriteList)).thenReturn(expectedFavoriteListDto);

        FavoriteListDto result = addProductsToFavoriteList.add(listOfFavoriteProducts, userId);

        assertEquals(expectedFavoriteListDto, result);

        verify(getFavoriteList, times(1)).getEntityFavoriteList(userId);
        verify(productInfoRepository, times(1)).findAllById(any());
        verify(favoriteRepository, times(1)).save(favoriteList);
        verify(favoriteListDtoConverter, times(1)).toDto(favoriteList);
    }
}
package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItem;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddProductsToFavoriteListTest {

    @InjectMocks
    private AddProductsToFavoriteList addProductsToFavoriteList;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private FavoriteListDtoConverter favoriteListDtoConverter;

    @Test
    @DisplayName("Add should return the FavoriteListDto with increased list of items when the listOfFavoriteProducts is valid")
    void shouldItemsAddToFavoriteListDtoWithValidProductIds() {
        UUID userId = UUID.randomUUID();

        FavoriteList favoriteList = new FavoriteList();
        favoriteList.setId(UUID.randomUUID());
        favoriteList.setFavoriteItems(new HashSet<>());

        ListOfFavoriteProducts listOfFavoriteProducts = new ListOfFavoriteProducts();
        listOfFavoriteProducts.setProductIds(Collections.singletonList(UUID.randomUUID()));

        FavoriteItem favoriteItemToAdd = new FavoriteItem();
        favoriteItemToAdd.setId(UUID.randomUUID());
        FavoriteItemDto favoriteItemDtoToAdd = new FavoriteItemDto(
                favoriteItemToAdd.getId(),
                any()
        );

        FavoriteList updatedFavoriteList = new FavoriteList();
        updatedFavoriteList.setId(favoriteList.getId());
        updatedFavoriteList.setFavoriteItems(Collections.singleton(favoriteItemToAdd));

        FavoriteListDto expectedFavoriteListDto = new FavoriteListDto(
                favoriteList.getId(),
                userId,
                new HashSet<>(Collections.singletonList(favoriteItemDtoToAdd)),                OffsetDateTime.now()
        );

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(favoriteList));
        when(productInfoRepository.findAllById(any())).thenReturn(Collections.singletonList(favoriteItemToAdd.getProductInfo()));
        when(favoriteRepository.save(favoriteList)).thenReturn(updatedFavoriteList);
        when(favoriteListDtoConverter.toDto(updatedFavoriteList)).thenReturn(expectedFavoriteListDto);

        FavoriteListDto result = addProductsToFavoriteList.add(listOfFavoriteProducts, userId);

        assertEquals(result, expectedFavoriteListDto);

        verify(favoriteRepository, times(1)).findByUserId(userId);
        verify(favoriteRepository, times(1)).save(favoriteList);
        verify(favoriteListDtoConverter, times(1)).toDto(updatedFavoriteList);
    }
}
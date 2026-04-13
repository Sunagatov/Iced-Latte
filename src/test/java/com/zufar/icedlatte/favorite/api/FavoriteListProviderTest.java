package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.converter.ListOfFavoriteProductsDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteListProviderTest {

    @InjectMocks
    private FavoriteListProvider favoriteListProvider;

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private FavoriteListDtoConverter favoriteListDtoConverter;
    @Mock
    private ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    @Mock
    private ProductPictureLinkUpdater productPictureLinkUpdater;

    private final OffsetDateTime beforeLaunchTime = OffsetDateTime.now().minusSeconds(1);

    @Test
    @DisplayName("Should get favorite list if it exists")
    void shouldGetFavoriteListIfItExists() {
        UUID userId = UUID.randomUUID();
        FavoriteListEntity expectedFavoriteList = new FavoriteListEntity();

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(expectedFavoriteList));

        FavoriteListEntity result = favoriteListProvider.getFavoriteListEntity(userId);

        assertEquals(expectedFavoriteList, result);
        verify(favoriteRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should create new favorite list if it doesn't exist")
    void shouldCreateNewFavoriteListIfItDoesntExist() {
        UUID userId = UUID.randomUUID();

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(favoriteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FavoriteListEntity result = favoriteListProvider.getFavoriteListEntity(userId);

        verify(favoriteRepository).findByUserId(userId);

        assertEquals(userId, result.getUserId());
        assertTrue(result.getFavoriteItems().isEmpty());
        assertTrue(beforeLaunchTime.isBefore(result.getUpdatedAt()));
    }

    @Test
    @DisplayName("Should get enriched favorite list when list exists")
    void shouldGetEnrichedFavoriteListWhenExists() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        FavoriteListEntity favoriteList = new FavoriteListEntity();
        FavoriteListDto dto = new FavoriteListDto(id, userId, new HashSet<>(), OffsetDateTime.now());
        com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto response =
                new com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto();
        response.setProducts(List.of());

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(favoriteList));
        when(favoriteListDtoConverter.toDto(favoriteList)).thenReturn(dto);
        when(listOfFavoriteProductsDtoConverter.toListProductDto(dto)).thenReturn(response);

        var result = favoriteListProvider.getEnrichedFavoriteList(userId);

        assertEquals(response, result);
        verify(favoriteRepository).findByUserId(userId);
        verify(favoriteListDtoConverter).toDto(favoriteList);
    }
}
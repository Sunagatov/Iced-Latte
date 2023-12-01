package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetFavoritesProductsByUserIdTest {

    @InjectMocks
    private GetFavoritesProductsByUserId getFavoritesProductsByUserId;

    @Mock
    private GetFavoriteList getFavoriteList;

    @Mock
    private FavoriteListDtoConverter favoriteListDtoConverter;

    @Test
    @DisplayName("Should get favorite list dto")
    void shouldGetFavoriteListDto() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        FavoriteList favoriteList = new FavoriteList();

        FavoriteListDto expectedFavoriteListDto = new FavoriteListDto(
                id,
                userId,
                new HashSet<>(),
                OffsetDateTime.now()
        );

        when(getFavoriteList.getEntityFavoriteList(userId)).thenReturn(favoriteList);
        when(favoriteListDtoConverter.toDto(favoriteList)).thenReturn(expectedFavoriteListDto);

        FavoriteListDto result = getFavoritesProductsByUserId.get(userId);

        assertEquals(expectedFavoriteListDto, result);
        verify(getFavoriteList, times(1)).getEntityFavoriteList(userId);
        verify(favoriteListDtoConverter, times(1)).toDto(favoriteList);
    }

}
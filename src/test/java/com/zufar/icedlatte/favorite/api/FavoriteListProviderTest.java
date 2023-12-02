package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FavoriteListProviderTest {

    @InjectMocks
    private FavoriteListProvider favoriteListProvider;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private SingleUserProvider singleUserProvider;

    @Mock
    private FavoriteListDtoConverter favoriteListDtoConverter;

    private final OffsetDateTime beforeLaunchTime = OffsetDateTime.now().minusSeconds(1);

    @Test
    @DisplayName("Should get favorite list if it exists")
    void shouldGetFavoriteListIfItExists() {
        UUID userId = UUID.randomUUID();
        FavoriteListEntity expectedFavoriteList = new FavoriteListEntity();

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(expectedFavoriteList));

        FavoriteListEntity result = favoriteListProvider.getFavoriteListEntity(userId);

        assertEquals(expectedFavoriteList, result);
        verify(favoriteRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should create new favorite list if it doesn't exist")
    void shouldCreateNewFavoriteListIfItDoesntExist() {
        UUID userId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);

        FavoriteListEntity result = favoriteListProvider.getFavoriteListEntity(userId);

        verify(favoriteRepository, times(1)).findByUserId(userId);
        assertEquals(userEntity, result.getUser());
        assertTrue(result.getFavoriteItems().isEmpty());
        assertTrue(beforeLaunchTime.isBefore(result.getUpdatedAt()));
    }

    @Test
    @DisplayName("Should get favorite list dto")
    void shouldGetFavoriteListDto() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FavoriteListEntity favoriteList = new FavoriteListEntity();
        FavoriteListDto expectedFavoriteList = new FavoriteListDto(
                id,
                userId,
                new HashSet<>(),
                OffsetDateTime.now()
        );

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.ofNullable(favoriteList));
        when(favoriteListDtoConverter.toDto(favoriteList)).thenReturn(expectedFavoriteList);

        FavoriteListDto result = favoriteListProvider.getFavoriteListDto(userId);

        assertEquals(expectedFavoriteList, result);

        verify(favoriteRepository, times(1)).findByUserId(userId);
        verify(favoriteListDtoConverter, times(1)).toDto(favoriteList);
    }
}
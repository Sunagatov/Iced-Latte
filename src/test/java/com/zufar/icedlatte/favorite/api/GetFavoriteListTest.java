package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteList;
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetFavoriteListTest {

    @InjectMocks
    private GetFavoriteList getFavoriteList;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private SingleUserProvider singleUserProvider;

    private final OffsetDateTime beforeLaunchTime = OffsetDateTime.now().minus(1, ChronoUnit.SECONDS);

    @Test
    @DisplayName("Should get favorite list if it exists")
    void shouldGetFavoriteListIfItExists() {
        UUID userId = UUID.randomUUID();
        FavoriteList expectedFavoriteList = new FavoriteList();

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.ofNullable(expectedFavoriteList));

        FavoriteList result = getFavoriteList.getEntityFavoriteList(userId);

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

        FavoriteList result = getFavoriteList.getEntityFavoriteList(userId);

        verify(favoriteRepository, times(1)).findByUserId(userId);
        assertEquals(userEntity, result.getUser());
        assertTrue(result.getFavoriteItems().isEmpty());
        assertTrue(beforeLaunchTime.isBefore(result.getUpdatedAt()));
    }

}
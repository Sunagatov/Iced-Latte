package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingCartCreator unit tests")
class ShoppingCartCreatorTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @InjectMocks
    private ShoppingCartCreator creator;

    @Test
    @DisplayName("getOrCreate returns existing cart without creating a new one")
    void getOrCreate_existingCart_returnsExisting() {
        UUID userId = UUID.randomUUID();
        ShoppingCart existing = ShoppingCart.builder().userId(userId).build();
        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(existing));

        ShoppingCart result = creator.getOrCreate(userId);

        assertThat(result).isEqualTo(existing);
        verify(shoppingCartRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate creates and saves new cart when none exists")
    void getOrCreate_noExistingCart_createsNew() {
        UUID userId = UUID.randomUUID();
        ShoppingCart newCart = ShoppingCart.builder().userId(userId).build();
        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.empty());
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(newCart);

        ShoppingCart result = creator.getOrCreate(userId);

        assertThat(result).isNotNull();
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getItemsQuantity()).isEqualTo(ShoppingCartCreator.DEFAULT_ITEMS_QUANTITY);
    }

    @Test
    @DisplayName("createNewShoppingCart initialises cart with empty items set for userId")
    void createNewShoppingCart_setsDefaults() {
        UUID userId = UUID.randomUUID();
        ShoppingCart saved = ShoppingCart.builder().userId(userId).items(new java.util.HashSet<>()).build();
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(saved);

        creator.createNewShoppingCart(userId);

        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartRepository).save(captor.capture());
        ShoppingCart built = captor.getValue();
        assertThat(built.getUserId()).isEqualTo(userId);
        assertThat(built.getItems()).isNotNull().isEmpty();
    }
}

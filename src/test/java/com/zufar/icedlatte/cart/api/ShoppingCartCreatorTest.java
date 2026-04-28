package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingCartCreator unit tests")
class ShoppingCartCreatorTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @InjectMocks
    private ShoppingCartCreator creator;

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("returns existing cart without creating a new one")
        void returnsExistingCartWithoutCreatingANewOne() {
            UUID userId = UUID.randomUUID();
            ShoppingCart existing = ShoppingCart.builder().userId(userId).build();
            when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(existing));

            ShoppingCart result = creator.getOrCreate(userId);

            assertThat(result).isSameAs(existing);
            verify(shoppingCartRepository).findShoppingCartByUserId(userId);
            verify(shoppingCartRepository, never()).save(any());
            verifyNoMoreInteractions(shoppingCartRepository);
        }

        @Test
        @DisplayName("creates and saves new cart when none exists")
        void createsAndSavesNewCartWhenNoneExists() {
            UUID userId = UUID.randomUUID();
            ShoppingCart newCart = ShoppingCart.builder().userId(userId).build();
            when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.empty());
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(newCart);

            ShoppingCart result = creator.getOrCreate(userId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
            verify(shoppingCartRepository).findShoppingCartByUserId(userId);
            verify(shoppingCartRepository).save(captor.capture());
            verifyNoMoreInteractions(shoppingCartRepository);
            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
            assertThat(captor.getValue().getItems()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("createNewShoppingCart")
    class CreateNewShoppingCart {

        @Test
        @DisplayName("initializes cart with empty items set for user id")
        void initializesCartWithEmptyItemsSetForUserId() {
            UUID userId = UUID.randomUUID();
            ShoppingCart saved = ShoppingCart.builder().userId(userId).items(new java.util.HashSet<>()).build();
            when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(saved);

            ShoppingCart result = creator.createNewShoppingCart(userId);

            assertThat(result).isNotNull();
            ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
            verify(shoppingCartRepository).save(captor.capture());
            verifyNoMoreInteractions(shoppingCartRepository);
            ShoppingCart built = captor.getValue();
            assertThat(built.getUserId()).isEqualTo(userId);
            assertThat(built.getItems()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("returns existing cart when concurrent insert causes uniqueness conflict")
        void returnsExistingCartWhenConcurrentInsertCausesUniquenessConflict() {
            UUID userId = UUID.randomUUID();
            ShoppingCart existing = ShoppingCart.builder().userId(userId).items(new java.util.HashSet<>()).build();
            when(shoppingCartRepository.save(any(ShoppingCart.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));
            when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(existing));

            ShoppingCart result = creator.createNewShoppingCart(userId);

            assertThat(result).isSameAs(existing);
            verify(shoppingCartRepository).save(any(ShoppingCart.class));
            verify(shoppingCartRepository).findShoppingCartByUserId(userId);
            verifyNoMoreInteractions(shoppingCartRepository);
        }

        @Test
        @DisplayName("throws when uniqueness conflict happens but cart still cannot be found")
        void throwsWhenConflictHappensButCartStillCannotBeFound() {
            UUID userId = UUID.randomUUID();
            when(shoppingCartRepository.save(any(ShoppingCart.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));
            when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> creator.createNewShoppingCart(userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cart not found after uniqueness conflict");
        }
    }
}

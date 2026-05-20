package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.openapi.dto.ReorderResponseDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderReorderService unit tests")
class OrderReorderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductCatalogApi productCatalogApi;
    @Mock private ShoppingCartService shoppingCartService;
    @InjectMocks private OrderReorderService reorderService;

    @Test
    @DisplayName("Adds available products to cart and reports unavailable ones")
    void reorderMixedAvailability() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID availableProductId = UUID.randomUUID();
        UUID unavailableProductId = UUID.randomUUID();

        OrderItem available = OrderItem.builder()
                .productId(availableProductId).productName("Nitro").productsQuantity(2).productPrice(BigDecimal.TEN).build();
        OrderItem unavailable = OrderItem.builder()
                .productId(unavailableProductId).productName("Discontinued").productsQuantity(1).productPrice(BigDecimal.ONE).build();
        Order order = Order.builder().id(orderId).userId(userId).items(List.of(available, unavailable)).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productCatalogApi.existsById(availableProductId)).thenReturn(true);
        when(productCatalogApi.existsById(unavailableProductId)).thenReturn(false);

        ShoppingCartDto cart = new ShoppingCartDto();
        cart.setId(UUID.randomUUID());
        when(shoppingCartService.addItems(eq(userId), any())).thenReturn(cart);

        ReorderResponseDto result = reorderService.reorder(orderId, userId);

        assertThat(result.getAddedItems()).isEqualTo(1);
        assertThat(result.getUnavailableItems()).hasSize(1);
        assertThat(result.getUnavailableItems().getFirst().getProductName()).isEqualTo("Discontinued");
        assertThat(result.getCartId()).isNotNull();
        verify(shoppingCartService).addItems(eq(userId), any());
    }

    @Test
    @DisplayName("Throws when user doesn't own the order")
    void reorderOtherUsersOrderThrows() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(UUID.randomUUID()).items(List.of()).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reorderService.reorder(orderId, UUID.randomUUID()))
                .isInstanceOf(OrderAccessDeniedException.class);
    }
}

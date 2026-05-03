package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.order.config.OrderConfig;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.validator.OrderAddressValidator;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreator unit tests")
class OrderCreatorTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderDtoConverter orderDtoConverter;
    @Mock private ShoppingCartService shoppingCartService;
    @Mock private ShoppingCartRepository shoppingCartRepository;
    @Mock private OrderAddressValidator orderAddressValidator;
    @Mock private DeliveryAddressRepository deliveryAddressRepository;
    @Mock private ProductInfoRepository productInfoRepository;
    @Mock private OrderConfig orderConfig;
    @Mock @SuppressWarnings("unused") private OrderProvider orderProvider;
    @Mock @SuppressWarnings("unused") private SingleUserProvider singleUserProvider;
    @InjectMocks private OrderCreator orderCreator;

    @Test
    @DisplayName("Throws BadRequestException when cart is empty")
    void createEmptyCartThrows() {
        UUID userId = UUID.randomUUID();
        CreateNewOrderRequestDto request = buildRequest(null, buildAddressDto());

        ShoppingCartDto emptyCart = new ShoppingCartDto();
        emptyCart.setItems(Collections.emptyList());
        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(emptyCart);

        assertThatThrownBy(() -> orderCreator.create(userId, request, null))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creates order with inline address")
    void createWithInlineAddress() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateNewOrderRequestDto request = buildRequest(null, buildAddressDto());
        ShoppingCartDto cart = buildCart();
        Order saved = Order.builder().id(UUID.randomUUID()).userId(userId).status(OrderStatus.CREATED).items(List.of()).build();
        OrderItem orderItem = OrderItem.builder().productId(productId).productName("Test").build();

        when(orderConfig.getCancellationWindowMinutes()).thenReturn(30);
        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(orderDtoConverter.toOrderItem(any())).thenReturn(orderItem);
        when(productInfoRepository.existsById(productId)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderDtoConverter.toResponseDto(saved)).thenReturn(new OrderDto());

        orderCreator.create(userId, request, null);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryAddress().getCountry()).isEqualTo("UK");
        verify(shoppingCartRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("Creates order with saved delivery address ID")
    void createWithDeliveryAddressId() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateNewOrderRequestDto request = buildRequest(addressId, null);
        ShoppingCartDto cart = buildCart();
        Order saved = Order.builder().id(UUID.randomUUID()).userId(userId).status(OrderStatus.CREATED).items(List.of()).build();
        OrderItem orderItem = OrderItem.builder().productId(productId).productName("Test").build();

        DeliveryAddressEntity savedAddr = DeliveryAddressEntity.builder()
                .id(addressId).country("DE").city("Berlin").line("Unter den Linden 1").postcode("10117").build();
        when(orderConfig.getCancellationWindowMinutes()).thenReturn(30);
        when(deliveryAddressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(savedAddr));
        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(orderDtoConverter.toOrderItem(any())).thenReturn(orderItem);
        when(productInfoRepository.existsById(productId)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderDtoConverter.toResponseDto(saved)).thenReturn(new OrderDto());

        orderCreator.create(userId, request, null);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryAddress().getCountry()).isEqualTo("DE");
        assertThat(captor.getValue().getDeliveryAddress().getCity()).isEqualTo("Berlin");
    }

    @Test
    @DisplayName("Throws when delivery address ID not found for user")
    void createWithInvalidDeliveryAddressIdThrows() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateNewOrderRequestDto request = buildRequest(addressId, null);
        ShoppingCartDto cart = buildCart();
        OrderItem orderItem = OrderItem.builder().productId(productId).productName("Test").build();

        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(orderDtoConverter.toOrderItem(any())).thenReturn(orderItem);
        when(productInfoRepository.existsById(productId)).thenReturn(true);
        when(deliveryAddressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderCreator.create(userId, request, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Delivery address not found");
    }

    @Test
    @DisplayName("Returns existing order when idempotency key matches")
    void createWithDuplicateIdempotencyKeyReturnsExisting() {
        UUID userId = UUID.randomUUID();
        String key = "idem-key-123";
        Order existing = Order.builder().id(UUID.randomUUID()).userId(userId).status(OrderStatus.CREATED).items(List.of()).build();
        OrderDto expectedDto = new OrderDto();

        when(orderRepository.findByIdempotencyKeyAndUserId(key, userId)).thenReturn(Optional.of(existing));
        when(orderDtoConverter.toResponseDto(existing)).thenReturn(expectedDto);

        OrderDto result = orderCreator.create(userId, buildRequest(null, buildAddressDto()), key);

        assertThat(result).isEqualTo(expectedDto);
        verify(orderRepository, never()).save(any());
    }

    private CreateNewOrderRequestDto buildRequest(UUID deliveryAddressId, AddressDto address) {
        CreateNewOrderRequestDto req = new CreateNewOrderRequestDto();
        req.setDeliveryAddressId(deliveryAddressId);
        req.setAddress(address);
        req.setRecipientName("John");
        req.setRecipientSurname("Doe");
        return req;
    }

    private AddressDto buildAddressDto() {
        AddressDto addr = new AddressDto();
        addr.setCountry("UK");
        addr.setCity("London");
        addr.setLine("123 Main St");
        addr.setPostcode("SW1A 1AA");
        return addr;
    }

    private ShoppingCartDto buildCart() {
        ShoppingCartDto cart = new ShoppingCartDto();
        cart.setItems(List.of(new ShoppingCartItemDto()));
        cart.setItemsQuantity(1);
        cart.setItemsTotalPrice(BigDecimal.TEN);
        return cart;
    }
}

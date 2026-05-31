package com.zufar.icedlatte.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.order.api.dto.CheckoutOrderRequest;
import com.zufar.icedlatte.order.api.dto.OrderAddressRequest;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.service.query.OrderDetailProvider;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import com.zufar.icedlatte.user.api.UserAddressApi;
import com.zufar.icedlatte.user.api.UserAddressSnapshot;
import com.zufar.icedlatte.user.service.SingleUserProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreator unit tests")
class OrderCreatorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDtoConverter orderDtoConverter;

    @Mock
    private CartCheckoutApi shoppingCartService;

    @Mock
    private UserAddressApi userAddressApi;

    @Mock
    private ProductCatalogApi productCatalogApi;

    @Mock
    @SuppressWarnings("unused")
    private OrderDetailProvider orderDetailProvider;

    @Mock
    @SuppressWarnings("unused")
    private SingleUserProvider singleUserProvider;

    @InjectMocks
    private OrderCreator orderCreator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderCreator, "cancellationWindowMinutes", 30);
        lenient().when(orderDtoConverter.toOrderItems(any())).thenAnswer(inv -> {
            List<CartItemSnapshot> items = inv.getArgument(0);
            return items.stream()
                    .map(i -> OrderItem.builder()
                            .productId(i.product().id())
                            .productName(i.product().name())
                            .productPrice(i.product().price())
                            .productsQuantity(i.productQuantity())
                            .build())
                    .toList();
        });
    }

    @Test
    @DisplayName("Throws BadRequestException when cart is empty")
    void createEmptyCartThrows() {
        UUID userId = UUID.randomUUID();
        CreateNewOrderRequestDto request = buildRequest(null, buildAddressDto());

        CartSnapshot emptyCart = new CartSnapshot(
                UUID.randomUUID(), userId, Collections.emptyList(), 0, BigDecimal.ZERO, 0, OffsetDateTime.now(), null);
        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(emptyCart);

        assertThatThrownBy(() -> orderCreator.create(userId, request, null)).isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creates order with inline address")
    void createWithInlineAddress() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateNewOrderRequestDto request = buildRequest(null, buildAddressDto());
        CartSnapshot cart = buildCart(productId);
        Order saved = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(List.of())
                .build();

        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(productCatalogApi.findExistingProductIds(Set.of(productId))).thenReturn(Set.of(productId));
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderDtoConverter.toResponseDto(saved)).thenReturn(new OrderDto());

        orderCreator.create(userId, request, null);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryAddress().getCountry()).isEqualTo("UK");
        verify(shoppingCartService).deleteCartForUser(userId);
    }

    @Test
    @DisplayName("Creates order with saved delivery address ID")
    void createWithDeliveryAddressId() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateNewOrderRequestDto request = buildRequest(addressId, null);
        CartSnapshot cart = buildCart(productId);
        Order saved = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(List.of())
                .build();

        UserAddressSnapshot savedAddr = new UserAddressSnapshot("DE", "Berlin", "Unter den Linden 1", "10117");
        when(userAddressApi.getDeliveryAddress(userId, addressId)).thenReturn(savedAddr);
        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(productCatalogApi.findExistingProductIds(Set.of(productId))).thenReturn(Set.of(productId));
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
        CartSnapshot cart = buildCart(productId);

        when(shoppingCartService.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(productCatalogApi.findExistingProductIds(Set.of(productId))).thenReturn(Set.of(productId));
        when(userAddressApi.getDeliveryAddress(userId, addressId))
                .thenThrow(new NotFoundException("Delivery address not found."));

        assertThatThrownBy(() -> orderCreator.create(userId, request, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Delivery address not found");
    }

    @Test
    @DisplayName("Returns existing order when idempotency key matches")
    void createWithDuplicateIdempotencyKeyReturnsExisting() {
        UUID userId = UUID.randomUUID();
        String key = "idem-key-123";
        Order existing = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(List.of())
                .build();
        OrderDto expectedDto = new OrderDto();

        when(orderRepository.findByIdempotencyKeyAndUserId(key, userId)).thenReturn(Optional.of(existing));
        when(orderDtoConverter.toResponseDto(existing)).thenReturn(expectedDto);

        OrderDto result = orderCreator.create(userId, buildRequest(null, buildAddressDto()), key);

        assertThat(result).isEqualTo(expectedDto);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checkout: rejects when both deliveryAddressId and address are provided")
    void createPendingPaymentOrder_bothAddressInputs_throws() {
        UUID userId = UUID.randomUUID();
        CreateCheckoutRequestDto req = new CreateCheckoutRequestDto()
                .recipientName("A")
                .recipientSurname("B")
                .deliveryAddressId(UUID.randomUUID())
                .address(buildAddressDto());

        assertThatThrownBy(() -> orderCreator.createPendingPaymentOrder(
                        userId, toCheckoutOrderRequest(req), buildCart(UUID.randomUUID())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not both");
    }

    @Test
    @DisplayName("Checkout: rejects when neither deliveryAddressId nor address is provided")
    void createPendingPaymentOrder_noAddress_throws() {
        UUID userId = UUID.randomUUID();
        CreateCheckoutRequestDto req =
                new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B");

        assertThatThrownBy(() -> orderCreator.createPendingPaymentOrder(
                        userId, toCheckoutOrderRequest(req), buildCart(UUID.randomUUID())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be provided");
    }

    @Test
    @DisplayName("Checkout: rejects when product is no longer available")
    void createPendingPaymentOrder_unavailableProduct_throws() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateCheckoutRequestDto req = new CreateCheckoutRequestDto()
                .recipientName("A")
                .recipientSurname("B")
                .address(buildAddressDto());
        CartSnapshot cart = buildCart(productId);

        when(productCatalogApi.findExistingProductIds(Set.of(productId))).thenReturn(Set.of());

        assertThatThrownBy(() -> orderCreator.createPendingPaymentOrder(userId, toCheckoutOrderRequest(req), cart))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer available");
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

    private CartSnapshot buildCart(UUID productId) {
        ProductSnapshot product = new ProductSnapshot(productId, "Test", "Desc", BigDecimal.TEN, 10, true, null);
        return new CartSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new CartItemSnapshot(UUID.randomUUID(), product, 1)),
                1,
                BigDecimal.TEN,
                1,
                OffsetDateTime.now(),
                null);
    }

    private static CheckoutOrderRequest toCheckoutOrderRequest(CreateCheckoutRequestDto req) {
        AddressDto a = req.getAddress();
        OrderAddressRequest addr = a == null
                ? null
                : new OrderAddressRequest(
                        Objects.requireNonNull(a.getCountry()),
                        Objects.requireNonNull(a.getCity()),
                        Objects.requireNonNull(a.getLine()),
                        Objects.requireNonNull(a.getPostcode()));
        return new CheckoutOrderRequest(
                req.getRecipientName(),
                req.getRecipientSurname(),
                req.getRecipientPhone(),
                req.getDeliveryAddressId(),
                addr);
    }
}

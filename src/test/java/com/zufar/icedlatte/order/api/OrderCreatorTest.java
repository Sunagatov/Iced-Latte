package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.cart.api.ShoppingCartProvider;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreator unit tests")
class OrderCreatorTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderDtoConverter orderDtoConverter;
    @Mock
    private ShoppingCartProvider shoppingCartProvider;
    @Mock
    @SuppressWarnings("unused") // required by @InjectMocks (@RequiredArgsConstructor), never called in tested paths
    private SingleUserProvider singleUserProvider;
    @InjectMocks
    private OrderCreator orderCreator;

    @Test
    @DisplayName("Creates order from cart and returns DTO")
    void create_validRequest_savesOrderAndReturnsDto() {
        UUID userId = UUID.randomUUID();

        ShoppingCartItemDto cartItem = new ShoppingCartItemDto();
        ShoppingCartDto cart = new ShoppingCartDto();
        cart.setItems(List.of(cartItem));
        cart.setItemsQuantity(1);
        cart.setItemsTotalPrice(BigDecimal.TEN);

        AddressDto addressDto = new AddressDto();
        addressDto.setCountry("US");
        addressDto.setCity("NYC");
        addressDto.setLine("123 Main St");
        addressDto.setPostcode("10001");

        CreateNewOrderRequestDto request = new CreateNewOrderRequestDto();
        request.setAddress(addressDto);
        request.setRecipientName("John");
        request.setRecipientSurname("Doe");
        request.setRecipientPhone("+12025550123");

        OrderItem orderItem = new OrderItem();
        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(List.of(orderItem))
                .build();
        OrderDto expectedDto = new OrderDto();

        when(shoppingCartProvider.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(orderDtoConverter.toOrderItem(cartItem)).thenReturn(orderItem);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderDtoConverter.toResponseDto(savedOrder)).thenReturn(expectedDto);

        OrderDto result = orderCreator.create(userId, request);

        assertThat(result).isEqualTo(expectedDto);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getUserId()).isEqualTo(userId);
        assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(capturedOrder.getRecipientName()).isEqualTo("John");
        assertThat(capturedOrder.getItemsQuantity()).isEqualTo(1);
        assertThat(capturedOrder.getItemsTotalPrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(capturedOrder.getSessionId()).isNotBlank();
    }

    @Test
    @DisplayName("Delivery address fields are mapped correctly from request")
    void create_mapsDeliveryAddressFromRequest() {
        UUID userId = UUID.randomUUID();

        ShoppingCartItemDto cartItem = new ShoppingCartItemDto();
        ShoppingCartDto cart = new ShoppingCartDto();
        cart.setItems(List.of(cartItem));
        cart.setItemsQuantity(1);
        cart.setItemsTotalPrice(BigDecimal.ONE);

        AddressDto addressDto = new AddressDto();
        addressDto.setCountry("DE");
        addressDto.setCity("Berlin");
        addressDto.setLine("Unter den Linden 1");
        addressDto.setPostcode("10117");

        CreateNewOrderRequestDto request = new CreateNewOrderRequestDto();
        request.setAddress(addressDto);
        request.setRecipientName("Anna");
        request.setRecipientSurname("Schmidt");

        Order savedOrder = Order.builder().id(UUID.randomUUID()).build();
        when(shoppingCartProvider.getByUserIdOrThrow(userId)).thenReturn(cart);
        when(orderDtoConverter.toOrderItem(cartItem)).thenReturn(new OrderItem());
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderDtoConverter.toResponseDto(savedOrder)).thenReturn(new OrderDto());

        orderCreator.create(userId, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryAddress().getCountry()).isEqualTo("DE");
        assertThat(captor.getValue().getDeliveryAddress().getCity()).isEqualTo("Berlin");
        assertThat(captor.getValue().getDeliveryAddress().getPostcode()).isEqualTo("10117");
    }
}

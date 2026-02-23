package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.cart.api.ShoppingCartProvider;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreator {

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final ShoppingCartProvider shoppingCartProvider;
    private final SingleUserProvider singleUserProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public OrderDto create(final UUID userId, final CreateNewOrderRequestDto request) {
        ShoppingCartDto cart = shoppingCartProvider.getByUserIdOrThrow(userId);

        List<OrderItem> items = cart.getItems().stream()
                .map(orderDtoConverter::toOrderItem)
                .toList();

        var requestAddress = request.getAddress();
        Address deliveryAddress = Address.builder()
                .country(requestAddress.getCountry())
                .city(requestAddress.getCity())
                .line(requestAddress.getLine())
                .postcode(requestAddress.getPostcode())
                .build();

        Order order = Order.builder()
                .userId(userId)
                .sessionId(UUID.randomUUID().toString())
                .status(OrderStatus.CREATED)
                .items(items)
                .deliveryAddress(deliveryAddress)
                .recipientName(request.getRecipientName())
                .recipientSurname(request.getRecipientSurname())
                .recipientPhone(request.getRecipientPhone())
                .itemsQuantity(cart.getItemsQuantity())
                .itemsTotalPrice(cart.getItemsTotalPrice())
                .build();

        Order saved = orderRepository.save(order);
        log.info("order.created: orderId={}, userId={}", saved.getId(), userId);
        return orderDtoConverter.toResponseDto(saved);
    }
}

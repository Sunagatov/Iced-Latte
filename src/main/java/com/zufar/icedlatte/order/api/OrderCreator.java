package com.zufar.icedlatte.order.api;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.ShoppingCartProvider;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
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
import com.zufar.icedlatte.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreator {

    private final OrderRepository orderRepository;
    private final OrderProvider orderProvider;
    private final OrderDtoConverter orderDtoConverter;
    private final ShoppingCartProvider shoppingCartProvider;
    private final ShoppingCartRepository shoppingCartRepository;
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

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public boolean createOrderAndDeleteCart(Session stripeSession) {
        String sessionId = stripeSession.getId();
        log.info("order.session.handling: sessionId={}", StringUtils.left(StringUtils.overlay(sessionId, "****", 6, sessionId.length()), 10));

        UUID userId = UUID.fromString(stripeSession.getMetadata().get("userId"));

        Optional<Order> existingOrder = orderProvider.getOrderEntityByUserAndSession(userId, sessionId);
        if (existingOrder.isPresent()) {
            log.info("order.session.already_handled: sessionId={}", StringUtils.left(StringUtils.overlay(sessionId, "****", 6, sessionId.length()), 10));
            return false;
        }

        ShoppingCartDto shoppingCartDto = shoppingCartProvider.getByUserIdOrThrow(userId);
        UserEntity user = singleUserProvider.getUserEntityById(userId);

        log.info("order.creating: userId={}", userId);
        Order orderEntity = createOrderEntityFromSession(user, shoppingCartDto, sessionId);
        orderRepository.saveAndFlush(orderEntity);
        log.info("order.created: userId={}", userId);

        shoppingCartRepository.deleteByUserId(userId);
        log.info("cart.deleted: userId={}", userId);

        return true;
    }

    private Order createOrderEntityFromSession(final UserEntity user, final ShoppingCartDto shoppingCartDto, final String sessionId) {
        List<OrderItem> shoppingOrderItems = shoppingCartDto.getItems().stream()
                .map(orderDtoConverter::toOrderItem)
                .toList();

        return Order.builder()
                .userId(user.getId())
                .sessionId(sessionId)
                .status(OrderStatus.CREATED)
                .deliveryAddress(user.getAddress())
                .itemsQuantity(shoppingCartDto.getItemsQuantity())
                .itemsTotalPrice(shoppingCartDto.getItemsTotalPrice())
                .items(shoppingOrderItems)
                .build();
    }
}

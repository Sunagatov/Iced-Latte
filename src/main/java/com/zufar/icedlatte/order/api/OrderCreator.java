package com.zufar.icedlatte.order.api;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.ShoppingCartProvider;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
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
    private final ShoppingCartProvider shoppingCartProvider;
    private final ShoppingCartRepository shoppingCartRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final SingleUserProvider singleUserProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public boolean createOrderAndDeleteCart(Session stripeSession) {
        String sessionId = stripeSession.getId();
        log.info("Handling completion of stripe checkout session with id = '{}'.", StringUtils.left(StringUtils.overlay(sessionId, "****", 6, sessionId.length()), 10));

        UUID userId = UUID.fromString(stripeSession.getMetadata().get("userId"));

        Optional<Order> existingOrder = orderProvider.getOrderEntityByUserAndSession(userId, sessionId);
        if (existingOrder.isPresent()) {
            log.info("Session completion has been already handled, order has been created");
            return false;
        }

        ShoppingCartDto shoppingCartDto = shoppingCartProvider.getByUserIdOrThrow(userId);
        UserEntity user = singleUserProvider.getUserEntityById(userId);

        log.info("Creating new order for user with id = '{}'", userId);
        Order orderEntity = createOrderEntity(user, shoppingCartDto, sessionId);
        orderRepository.saveAndFlush(orderEntity);
        log.info("New order created and saved to database for user");

        shoppingCartRepository.deleteByUserId(userId);
        log.info("Deleted the shopping cart for user");

        return true;
    }

    private Order createOrderEntity(final UserEntity user, final ShoppingCartDto shoppingCartDto, final String sessionId) {
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

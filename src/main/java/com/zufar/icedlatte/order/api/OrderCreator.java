package com.zufar.icedlatte.order.api;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.common.exception.BadRequestException;
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
    private final ShoppingCartService shoppingCartService;
    private final ShoppingCartRepository shoppingCartRepository;
    private final SingleUserProvider singleUserProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public OrderDto create(final UUID userId, final CreateNewOrderRequestDto request) {
        ShoppingCartDto cart = shoppingCartService.getByUserIdOrThrow(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException(String.format("Cannot create order: shopping cart is empty for userId=%s.", userId));
        }

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
        UUID userId = UUID.fromString(stripeSession.getMetadata().get("userId"));
        String maskedSessionId = maskSessionId(sessionId);

        Optional<Order> existingOrder = orderProvider.getOrderEntityByUserAndSession(userId, sessionId);
        if (existingOrder.isPresent()) {
            log.info("order.session.already_handled: userId={}, sessionId={}", userId, maskedSessionId);
            return false;
        }

        ShoppingCartDto shoppingCartDto = shoppingCartService.getByUserIdOrThrow(userId);
        UserEntity user = singleUserProvider.getUserEntityById(userId);

        Order orderEntity = createOrderEntityFromSession(user, shoppingCartDto, sessionId);
        Order savedOrder = orderRepository.saveAndFlush(orderEntity);
        shoppingCartRepository.deleteByUserId(userId);
        log.info("checkout.completed: userId={}, orderId={}, sessionId={}, itemsQuantity={}, amount={}",
                userId,
                savedOrder.getId(),
                maskedSessionId,
                savedOrder.getItemsQuantity(),
                savedOrder.getItemsTotalPrice());

        return true;
    }

    private Order createOrderEntityFromSession(final UserEntity user,
                                               final ShoppingCartDto shoppingCartDto,
                                               final String sessionId) {
        if (shoppingCartDto.getItems() == null || shoppingCartDto.getItems().isEmpty()) {
            throw new BadRequestException(String.format("Cannot create order: shopping cart is empty for userId=%s.", user.getId()));
        }

        List<OrderItem> shoppingOrderItems = shoppingCartDto.getItems().stream()
                .map(orderDtoConverter::toOrderItem)
                .toList();

        if (user.getAddress() == null) {
            throw new IllegalStateException("User does not have a delivery address. Cannot create order.");
        }
        
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

    private static String maskSessionId(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return "unknown";
        }
        return StringUtils.left(StringUtils.overlay(sessionId, "****", 6, sessionId.length()), 10);
    }
}

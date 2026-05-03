package com.zufar.icedlatte.order.api;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.util.SessionIdMasker;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.order.config.OrderConfig;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.validator.OrderAddressValidator;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private final OrderAddressValidator orderAddressValidator;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final ProductInfoRepository productInfoRepository;
    private final OrderConfig orderConfig;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public OrderDto create(final UUID userId, final CreateNewOrderRequestDto request,
                           final String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId);
            if (existing.isPresent()) {
                log.info("order.idempotent_hit: userId={}, idempotencyKey={}", userId, idempotencyKey);
                return orderDtoConverter.toResponseDto(existing.get());
            }
        }

        orderAddressValidator.validate(request);

        ShoppingCartDto cart = shoppingCartService.getByUserIdOrThrow(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot create order: shopping cart is empty for userId=" + userId);
        }

        List<OrderItem> items = cart.getItems().stream()
                .map(orderDtoConverter::toOrderItem)
                .toList();

        List<String> unavailable = items.stream()
                .filter(item -> !productInfoRepository.existsById(item.getProductId()))
                .map(OrderItem::getProductName)
                .toList();
        if (!unavailable.isEmpty()) {
            throw new BadRequestException("Products no longer available: " + String.join(", ", unavailable));
        }

        Address deliveryAddress = resolveAddress(request, userId);

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
                .cancellationDeadline(OffsetDateTime.now().plusMinutes(orderConfig.getCancellationWindowMinutes()))
                .idempotencyKey(idempotencyKey)
                .build();

        Order saved = orderRepository.save(order);
        shoppingCartRepository.deleteByUserId(userId);
        log.info("order.created: orderId={}, userId={}", saved.getId(), userId);
        return orderDtoConverter.toResponseDto(saved);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public boolean createOrderAndDeleteCart(Session stripeSession) {
        String sessionId = stripeSession.getId();
        UUID userId = UUID.fromString(stripeSession.getMetadata().get("userId"));
        String maskedSessionId = SessionIdMasker.mask(sessionId);

        Optional<Order> existingOrder = orderProvider.getOrderEntityByUserAndSession(userId, sessionId);
        if (existingOrder.isPresent()) {
            log.info("order.session.already_handled: userId={}, sessionId={}", userId, maskedSessionId);
            return false;
        }

        ShoppingCartDto cart = shoppingCartService.getByUserIdOrThrow(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot create order: shopping cart is empty for userId=" + userId);
        }

        UserEntity user = singleUserProvider.getUserEntityById(userId);
        Address deliveryAddress = resolveDefaultAddress(userId, user);

        List<OrderItem> items = cart.getItems().stream()
                .map(orderDtoConverter::toOrderItem)
                .toList();

        Order order = Order.builder()
                .userId(userId)
                .sessionId(sessionId)
                .status(OrderStatus.CREATED)
                .deliveryAddress(deliveryAddress)
                .recipientName(user.getFirstName() != null ? user.getFirstName() : "")
                .recipientSurname(user.getLastName() != null ? user.getLastName() : "")
                .recipientPhone(user.getPhoneNumber())
                .itemsQuantity(cart.getItemsQuantity())
                .itemsTotalPrice(cart.getItemsTotalPrice())
                .items(items)
                .cancellationDeadline(OffsetDateTime.now().plusMinutes(orderConfig.getCancellationWindowMinutes()))
                .build();

        Order saved = orderRepository.saveAndFlush(order);
        shoppingCartRepository.deleteByUserId(userId);
        log.info("checkout.completed: userId={}, orderId={}, sessionId={}",
                userId, saved.getId(), maskedSessionId);
        return true;
    }

    private Address resolveAddress(CreateNewOrderRequestDto request, UUID userId) {
        if (request.getDeliveryAddressId() != null) {
            DeliveryAddressEntity saved = deliveryAddressRepository
                    .findByIdAndUserId(request.getDeliveryAddressId(), userId)
                    .orElseThrow(() -> new BadRequestException(
                            "Delivery address not found: " + request.getDeliveryAddressId()));
            return snapshotAddress(saved);
        }
        AddressDto addr = request.getAddress();
        if (addr == null) {
            throw new BadRequestException("Either 'deliveryAddressId' or 'address' must be provided.");
        }
        return Address.builder()
                .country(addr.getCountry())
                .city(addr.getCity())
                .line(addr.getLine())
                .postcode(addr.getPostcode())
                .build();
    }

    private Address resolveDefaultAddress(UUID userId, UserEntity user) {
        List<DeliveryAddressEntity> addresses = deliveryAddressRepository.findAllByUserId(userId);
        Optional<DeliveryAddressEntity> defaultAddr = addresses.stream()
                .filter(DeliveryAddressEntity::isDefault)
                .findFirst();
        if (defaultAddr.isPresent()) {
            return snapshotAddress(defaultAddr.get());
        }
        if (!addresses.isEmpty()) {
            return snapshotAddress(addresses.getFirst());
        }
        if (user.getAddress() != null) {
            return user.getAddress();
        }
        throw new IllegalStateException("User does not have a delivery address. Cannot create order.");
    }

    private static Address snapshotAddress(DeliveryAddressEntity entity) {
        return Address.builder()
                .country(entity.getCountry())
                .city(entity.getCity())
                .line(entity.getLine())
                .postcode(entity.getPostcode())
                .build();
    }
}

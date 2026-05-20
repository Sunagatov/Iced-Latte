package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.order.api.OrderCheckoutApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.product.api.ProductService;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class OrderCreator implements OrderCheckoutApi {

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final ShoppingCartService shoppingCartService;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final ProductService productService;

    @Value("${order.cancellation-window-minutes:30}")
    private int cancellationWindowMinutes;

    @Override
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

        validateAddressInput(request);

        ShoppingCartDto cart = shoppingCartService.getByUserIdOrThrow(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot create order: shopping cart is empty for userId=" + userId);
        }

        List<OrderItem> items = cart.getItems().stream()
                .map(orderDtoConverter::toOrderItem)
                .toList();

        validateProductAvailability(items);

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
                .cancellationDeadline(OffsetDateTime.now().plusMinutes(cancellationWindowMinutes))
                .idempotencyKey(idempotencyKey)
                .build();

        Order saved = orderRepository.save(order);
        shoppingCartService.deleteCartForUser(userId);
        log.info("order.created: orderId={}, userId={}", saved.getId(), userId);
        return orderDtoConverter.toResponseDto(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public OrderSnapshot createPendingPaymentOrderSnapshot(UUID userId, CreateCheckoutRequestDto request,
                                                           ShoppingCartDto cart) {
        Order order = createPendingPaymentOrder(userId, request, cart);
        var items = order.getItems() == null ? List.<OrderSnapshot.OrderItemSnapshot>of()
                : order.getItems().stream()
                    .map(i -> new OrderSnapshot.OrderItemSnapshot(i.getProductName(), i.getProductPrice(), i.getProductsQuantity()))
                    .toList();
        return new OrderSnapshot(order.getId(), order.getUserId(), order.getStatus(),
                order.getItemsTotalPrice(), order.getStripePaymentIntentId(), items);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    Order createPendingPaymentOrder(UUID userId, CreateCheckoutRequestDto request, ShoppingCartDto cart) {
        validateCheckoutAddressInput(request.getDeliveryAddressId(), request.getAddress());

        Address deliveryAddress = resolveDeliveryAddress(
                request.getDeliveryAddressId(), request.getAddress(), userId);

        List<OrderItem> items = cart.getItems().stream()
                .map(orderDtoConverter::toOrderItem)
                .toList();

        validateProductAvailability(items);

        Order order = Order.builder()
                .userId(userId)
                .sessionId(UUID.randomUUID().toString())
                .status(OrderStatus.PENDING_PAYMENT)
                .items(items)
                .deliveryAddress(deliveryAddress)
                .recipientName(request.getRecipientName())
                .recipientSurname(request.getRecipientSurname())
                .recipientPhone(request.getRecipientPhone())
                .itemsQuantity(cart.getItemsQuantity())
                .itemsTotalPrice(cart.getItemsTotalPrice())
                .cancellationDeadline(OffsetDateTime.now().plusMinutes(cancellationWindowMinutes))
                .build();

        Order saved = orderRepository.save(order);
        log.info("order.pending_payment: orderId={}, userId={}", saved.getId(), userId);
        return saved;
    }

    private void validateProductAvailability(List<OrderItem> items) {
        List<String> unavailable = items.stream()
                .filter(item -> !productService.existsById(item.getProductId()))
                .map(OrderItem::getProductName)
                .toList();
        if (!unavailable.isEmpty()) {
            throw new BadRequestException("Products no longer available: " + String.join(", ", unavailable));
        }
    }

    private Address resolveAddress(CreateNewOrderRequestDto request, UUID userId) {
        return resolveDeliveryAddress(request.getDeliveryAddressId(), request.getAddress(), userId);
    }

    private Address resolveDeliveryAddress(UUID deliveryAddressId, AddressDto inlineAddress, UUID userId) {
        if (deliveryAddressId != null) {
            DeliveryAddressEntity saved = deliveryAddressRepository
                    .findByIdAndUserId(deliveryAddressId, userId)
                    .orElseThrow(() -> new BadRequestException(
                            "Delivery address not found: " + deliveryAddressId));
            return snapshotAddress(saved);
        }
        if (inlineAddress == null) {
            throw new BadRequestException("Either 'deliveryAddressId' or 'address' must be provided.");
        }
        return Address.builder()
                .country(inlineAddress.getCountry())
                .city(inlineAddress.getCity())
                .line(inlineAddress.getLine())
                .postcode(inlineAddress.getPostcode())
                .build();
    }

    private static Address snapshotAddress(DeliveryAddressEntity entity) {
        return Address.builder()
                .country(entity.getCountry())
                .city(entity.getCity())
                .line(entity.getLine())
                .postcode(entity.getPostcode())
                .build();
    }

    private static void validateAddressInput(CreateNewOrderRequestDto request) {
        validateCheckoutAddressInput(request.getDeliveryAddressId(), request.getAddress());
    }

    private static void validateCheckoutAddressInput(UUID deliveryAddressId, AddressDto inlineAddress) {
        boolean hasId = deliveryAddressId != null;
        boolean hasInline = inlineAddress != null;
        if (!hasId && !hasInline) {
            throw new BadRequestException("Either 'deliveryAddressId' or 'address' must be provided.");
        }
        if (hasId && hasInline) {
            throw new BadRequestException("Provide either 'deliveryAddressId' or 'address', not both.");
        }
    }
}

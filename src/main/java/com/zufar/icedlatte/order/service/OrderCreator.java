package com.zufar.icedlatte.order.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.api.OrderCheckoutApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.dto.CheckoutOrderRequest;
import com.zufar.icedlatte.order.api.dto.OrderAddressRequest;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderAddress;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.exception.OrderDeliveryAddressNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.user.api.UserAddressApi;
import com.zufar.icedlatte.user.api.UserAddressSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreator implements OrderCheckoutApi {

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final CartCheckoutApi cartCheckoutApi;
    private final UserAddressApi userAddressApi;
    private final ProductCatalogApi productCatalogApi;

    @Value("${order.cancellation-window-minutes:30}")
    private int cancellationWindowMinutes;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public OrderDto create(
            final UUID userId, final CreateNewOrderRequestDto request, final @Nullable String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId);
            if (existing.isPresent()) {
                log.info("order.idempotent_hit: userId={}, idempotencyKey={}", userId, idempotencyKey);
                return orderDtoConverter.toResponseDto(existing.get());
            }
        }

        validateAddressInput(request);

        CartSnapshot cart = cartCheckoutApi.getByUserIdOrThrow(userId);
        if (cart.items().isEmpty()) {
            throw new BadRequestException("Cannot create order: shopping cart is empty for userId=" + userId);
        }

        List<OrderItem> items = orderDtoConverter.toOrderItems(cart.items());
        validateProductAvailability(items);

        OrderAddress deliveryAddress = resolveAddress(request, userId);

        Order order = Order.builder()
                .userId(userId)
                .sessionId(UUID.randomUUID().toString())
                .status(OrderStatus.CREATED)
                .items(items)
                .deliveryAddress(deliveryAddress)
                .recipientName(request.getRecipientName())
                .recipientSurname(request.getRecipientSurname())
                .recipientPhone(request.getRecipientPhone())
                .itemsQuantity(cart.itemsQuantity())
                .itemsTotalPrice(cart.itemsTotalPrice())
                .cancellationDeadline(OffsetDateTime.now().plusMinutes(cancellationWindowMinutes))
                .idempotencyKey(idempotencyKey)
                .build();

        Order saved = orderRepository.save(order);
        cartCheckoutApi.deleteCartForUser(userId);
        log.info("order.created: orderId={}, userId={}", saved.getId(), userId);
        return orderDtoConverter.toResponseDto(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public OrderSnapshot createPendingPaymentOrderSnapshot(
            UUID userId, CheckoutOrderRequest request, CartSnapshot cart) {
        Order order = createPendingPaymentOrder(userId, request, cart);
        return orderDtoConverter.toSnapshot(order);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    Order createPendingPaymentOrder(UUID userId, CheckoutOrderRequest request, CartSnapshot cart) {
        validateCheckoutAddressInput(request.deliveryAddressId(), request.address() != null);

        OrderAddress deliveryAddress =
                resolveDeliveryAddress(request.deliveryAddressId(), toAddressFields(request.address()), userId);

        List<OrderItem> items = orderDtoConverter.toOrderItems(cart.items());
        validateProductAvailability(items);

        Order order = Order.builder()
                .userId(userId)
                .sessionId(UUID.randomUUID().toString())
                .status(OrderStatus.PENDING_PAYMENT)
                .items(items)
                .deliveryAddress(deliveryAddress)
                .recipientName(request.recipientName())
                .recipientSurname(request.recipientSurname())
                .recipientPhone(request.recipientPhone())
                .itemsQuantity(cart.itemsQuantity())
                .itemsTotalPrice(cart.itemsTotalPrice())
                .cancellationDeadline(OffsetDateTime.now().plusMinutes(cancellationWindowMinutes))
                .build();

        Order saved = orderRepository.save(order);
        log.info("order.pending_payment: orderId={}, userId={}", saved.getId(), userId);
        return saved;
    }

    private void validateProductAvailability(List<OrderItem> items) {
        Set<UUID> requestedProductIds =
                items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Set<UUID> existingProductIds = productCatalogApi.findExistingProductIds(requestedProductIds);
        List<String> unavailable = items.stream()
                .filter(item -> !existingProductIds.contains(item.getProductId()))
                .map(OrderItem::getProductName)
                .toList();
        if (!unavailable.isEmpty()) {
            throw new BadRequestException("Products no longer available: " + String.join(", ", unavailable));
        }
    }

    private OrderAddress resolveAddress(CreateNewOrderRequestDto request, UUID userId) {
        return resolveDeliveryAddress(request.getDeliveryAddressId(), toAddressFields(request.getAddress()), userId);
    }

    private OrderAddress resolveDeliveryAddress(
            @Nullable UUID deliveryAddressId, @Nullable AddressFields inlineAddress, UUID userId) {
        if (deliveryAddressId != null) {
            try {
                return snapshotAddress(userAddressApi.getDeliveryAddress(userId, deliveryAddressId));
            } catch (NotFoundException ex) {
                throw new OrderDeliveryAddressNotFoundException();
            }
        }
        if (inlineAddress == null) {
            throw new BadRequestException("Either 'deliveryAddressId' or 'address' must be provided.");
        }
        return OrderAddress.builder()
                .country(inlineAddress.country())
                .city(inlineAddress.city())
                .line(inlineAddress.line())
                .postcode(inlineAddress.postcode())
                .build();
    }

    private static OrderAddress snapshotAddress(UserAddressSnapshot snapshot) {
        return OrderAddress.builder()
                .country(snapshot.country())
                .city(snapshot.city())
                .line(snapshot.line())
                .postcode(snapshot.postcode())
                .build();
    }

    private static void validateAddressInput(CreateNewOrderRequestDto request) {
        validateCheckoutAddressInput(request.getDeliveryAddressId(), request.getAddress() != null);
    }

    private static void validateCheckoutAddressInput(@Nullable UUID deliveryAddressId, boolean hasInline) {
        boolean hasId = deliveryAddressId != null;
        if (!hasId && !hasInline) {
            throw new BadRequestException("Either 'deliveryAddressId' or 'address' must be provided.");
        }
        if (hasId && hasInline) {
            throw new BadRequestException("Provide either 'deliveryAddressId' or 'address', not both.");
        }
    }

    private static @Nullable AddressFields toAddressFields(@Nullable AddressDto address) {
        return address == null
                ? null
                : new AddressFields(
                        requireAddressPart(address.getCountry(), "country"),
                        requireAddressPart(address.getCity(), "city"),
                        requireAddressPart(address.getLine(), "line"),
                        requireAddressPart(address.getPostcode(), "postcode"));
    }

    private static @Nullable AddressFields toAddressFields(@Nullable OrderAddressRequest address) {
        return address == null
                ? null
                : new AddressFields(address.country(), address.city(), address.line(), address.postcode());
    }

    private static String requireAddressPart(@Nullable String value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("Address field '" + fieldName + "' must be provided.");
        }
        return value;
    }

    private record AddressFields(String country, String city, String line, String postcode) {}
}

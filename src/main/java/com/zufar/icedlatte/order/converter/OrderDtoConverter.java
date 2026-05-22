package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.OrderStatusSnapshot;
import com.zufar.icedlatte.order.api.dto.CheckoutOrderRequest;
import com.zufar.icedlatte.order.api.dto.OrderAddressRequest;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import org.jspecify.annotations.Nullable;
import org.mapstruct.*;

import java.util.List;

import static com.zufar.icedlatte.common.util.Preconditions.requireNonNullOrThrow;

@SuppressWarnings("NullableProblems")
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.FIELD)
public interface OrderDtoConverter {

    @Mapping(target = "canCancel", ignore = true)
    @Mapping(target = "canRefund", ignore = true)
    OrderDto toResponseDto(final Order orderEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", source = "product.price")
    @Mapping(target = "productsQuantity", source = "productQuantity")
    OrderItem toOrderItem(CartItemSnapshot item);

    List<OrderItem> toOrderItems(List<CartItemSnapshot> items);

    default OrderSnapshot toSnapshot(Order order) {
        List<OrderSnapshot.OrderItemSnapshot> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .map(i -> new OrderSnapshot.OrderItemSnapshot(i.getProductName(), i.getProductPrice(), i.getProductsQuantity()))
                .toList();

        OrderStatusSnapshot statusSnapshot = requireNonNullOrThrow(toStatusSnapshot(order.getStatus()),
                () -> new IllegalStateException("Order status must not be null for orderId: " + order.getId()));

        return new OrderSnapshot(order.getId(), order.getUserId(), statusSnapshot,
                order.getItemsTotalPrice(), order.getStripePaymentIntentId(), items);
    }

    default @Nullable OrderStatusSnapshot toStatusSnapshot(@Nullable OrderStatus status) {
        return status == null ? null : OrderStatusSnapshot.valueOf(status.name());
    }

    @Mapping(target = "address", source = "address")
    CheckoutOrderRequest toCheckoutOrderRequest(CreateCheckoutRequestDto request);

    @Mapping(target = "country", source = "country")
    OrderAddressRequest toAddressRequest(AddressDto address);
}

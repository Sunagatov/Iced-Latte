package com.zufar.icedlatte.order.converter;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.*;

import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.common.exception.BadRequestException;
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

@SuppressWarnings("NullableProblems")
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
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
                        .map(i -> new OrderSnapshot.OrderItemSnapshot(
                                i.getProductName(), i.getProductPrice(), i.getProductsQuantity()))
                        .toList();

        OrderStatus status = order.getStatus();
        if (status == null) {
            throw new IllegalStateException("Order status must not be null for orderId: " + order.getId());
        }
        OrderStatusSnapshot statusSnapshot = OrderStatusSnapshot.valueOf(status.name());

        return new OrderSnapshot(
                order.getId(),
                order.getUserId(),
                statusSnapshot,
                order.getItemsTotalPrice(),
                order.getStripePaymentIntentId(),
                items);
    }

    default CheckoutOrderRequest toCheckoutOrderRequest(CreateCheckoutRequestDto request) {
        return new CheckoutOrderRequest(
                request.getRecipientName(),
                request.getRecipientSurname(),
                request.getRecipientPhone(),
                request.getDeliveryAddressId(),
                toAddressRequest(request.getAddress()));
    }

    default @Nullable OrderAddressRequest toAddressRequest(@Nullable AddressDto address) {
        if (address == null) {
            return null;
        }
        return new OrderAddressRequest(
                requireAddressPart(address.getCountry(), "country"),
                requireAddressPart(address.getCity(), "city"),
                requireAddressPart(address.getLine(), "line"),
                requireAddressPart(address.getPostcode(), "postcode"));
    }

    private static String requireAddressPart(@Nullable String value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("Address field '" + fieldName + "' must be provided.");
        }
        return value;
    }
}

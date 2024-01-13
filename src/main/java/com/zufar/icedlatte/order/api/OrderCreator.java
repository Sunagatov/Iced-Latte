package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderItemRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.zufar.icedlatte.order.api.OrderItemsCalculator.calculate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreator {

    private final ProductInfoRepository productInfoRepository;
    private final OrderDtoConverter orderDtoConverter;
    public static final int DEFAULT_PRODUCTS_QUANTITY = 0;

    public Order createNewOrder(OrderRequestDto orderBody, UUID userId) {
        var order = orderDtoConverter.toOrderEntity(orderBody);
        var items = createOrderItems(order, orderBody.getItems());

        order.setUserId(userId);
        order.setItems(items);
        order.setItemsQuantity(calculate(items, DEFAULT_PRODUCTS_QUANTITY));
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(OffsetDateTime.now());

        return order;
    }

    private List<OrderItem> createOrderItems(Order order, List<OrderItemRequestDto> products) {
        Map<UUID, Integer> productsWithQuantity = products.stream()
                .collect(Collectors.toMap(OrderItemRequestDto::getProductId, OrderItemRequestDto::getProductQuantity));

        return productInfoRepository.findAllById(productsWithQuantity.keySet()).stream()
                .map(productInfo ->
                        OrderItem.builder()
                                .order(order)
                                .productQuantity(productsWithQuantity.get(productInfo.getProductId()))
                                .productInfo(productInfo)
                                .build()
                )
                .toList();
    }
}

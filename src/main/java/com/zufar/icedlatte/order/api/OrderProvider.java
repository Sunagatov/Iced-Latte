package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.zufar.icedlatte.order.api.Calculator.calculate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProvider {

    private final OrderRepository orderRepository;
    private final ProductInfoRepository productInfoRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    public static final int DEFAULT_PRODUCTS_QUANTITY = 0;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public AddedOrder addNewOrder(final OrderDto orderBody) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("User id: {}", userId);
        var order = createNewOrder(orderBody, userId);
        orderRepository.save(order);
        log.info("New order was created and saved to database.");
        return orderDtoConverter.toAddedOrder(order);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ListOfAddedOrders getOrdersByStatus(final Set<OrderStatus> statusList) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("User id: {}", userId);
        // TODO: should I create a custom SQL query, rather than filter here?
        var ordersStream = orderRepository.findAllByUserId(userId).stream();
        if (statusList != null) {
            ordersStream = ordersStream.filter(order -> statusList.contains(order.getStatus()));
        }
        var orders = ordersStream.map(orderDtoConverter::toAddedOrder).toList();
        return orderDtoConverter.toList(orders);
    }

    private Order createNewOrder(OrderDto orderBody, UUID userId) {
        var order = orderDtoConverter.toOrder(orderBody);
        var items = createOrderItems(order, orderBody.getItems());
        var totalProductsCost = calculate(items);
        var itemsQuantity = items.stream().map(OrderItem::getProductQuantity)
                .reduce(Integer::sum).orElse(DEFAULT_PRODUCTS_QUANTITY);

        order.setUserId(userId);
        order.setItems(items);
        order.setItemsQuantity(itemsQuantity);
        order.setTotalProductsCost(totalProductsCost);
        order.setTotalOrderCost(calculate(totalProductsCost, orderBody.getTaxCost(), orderBody.getDeliveryCost()));
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(OffsetDateTime.now());

        return order;
    }

    private Set<OrderItem> createOrderItems(Order order, List<OrderItemDto> products) {
        Map<UUID, Integer> productsWithQuantity = products.stream()
                .collect(Collectors.toMap(OrderItemDto::getProductId, OrderItemDto::getProductQuantity));

        return productInfoRepository.findAllById(productsWithQuantity.keySet()).stream()
                .map(productInfo ->
                        OrderItem.builder()
                                .order(order)
                                .productQuantity(productsWithQuantity.get(productInfo.getProductId()))
                                .productInfo(productInfo)
                                .build()
                )
                .collect(Collectors.toSet());
    }
}

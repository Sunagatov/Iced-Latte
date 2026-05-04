package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.openapi.dto.CheckoutStatusDto;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Read-only service for the success page to poll payment status.
 * Does not create or modify anything.
 */
@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    public CheckoutStatusDto getStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        UserDto currentUser = securityPrincipalProvider.get();
        if (!order.getUserId().equals(currentUser.getId())) {
            throw new OrderAccessDeniedException(orderId);
        }

        CheckoutStatusDto dto = new CheckoutStatusDto()
                .orderId(order.getId())
                .orderStatus(order.getStatus());

        paymentRepository.findByOrderId(orderId)
                .ifPresent(p -> dto.paymentStatus(
                        CheckoutStatusDto.PaymentStatusEnum.fromValue(p.getStatus().name())));

        return dto;
    }
}

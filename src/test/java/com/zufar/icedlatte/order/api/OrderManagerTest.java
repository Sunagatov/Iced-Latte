package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.AddedOrder;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.createOrderDto;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("OrderManager Tests")
@ExtendWith(MockitoExtension.class)
class OrderManagerTest {

    @InjectMocks
    private OrderManager orderManager;
    @Inject
    private OrderDtoConverter converter;
    @Mock
    private ProductInfoRepository productInfoRepository;

    @Test
    @DisplayName("addOrder should return the AddedOrder with order ID")
    void shouldAddOrder() {
//        var id = UUID.randomUUID();
//        AddedOrder result = orderManager.addNewOrder(createOrderDto());
//        AddedOrder expected = new AddedOrder(id, "CREATED");
//        assertEquals(expected, result);
    }
}

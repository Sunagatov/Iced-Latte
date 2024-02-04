package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.OrderItemResponseDto;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderItemDtoConverter {

    private final ProductInfoDtoConverter productInfoDtoConverter;

    @Named("toOrderItemResponseDto")
    OrderItemResponseDto toDto(final OrderItem entity) {
        if ( entity == null ) {
            return null;
        }

        OrderItemResponseDto orderItemResponseDto = new OrderItemResponseDto();

        orderItemResponseDto.setProductInfo( productInfoDtoConverter.toDto( entity.getProductInfo() ) );
        orderItemResponseDto.setId( entity.getId() );
        orderItemResponseDto.setProductQuantity( entity.getProductQuantity() );

        return orderItemResponseDto;
    }
}

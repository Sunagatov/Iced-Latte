package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.OrderItemResponseDto;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = ProductInfoDtoConverter.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.FIELD)
public interface OrderItemDtoConverter {

    @Named("toOrderItemResponseDto")
    @Mapping(target = "productInfo", source = "entity.productInfo", qualifiedByName = {"toProductInfoDto"})
    OrderItemResponseDto toDto(final OrderItem entity);
}

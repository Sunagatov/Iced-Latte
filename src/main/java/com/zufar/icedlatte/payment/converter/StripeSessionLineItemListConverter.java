package com.zufar.icedlatte.payment.converter;

import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.payment.config.StripeProperties;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        imports = BigDecimal.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
@SuppressWarnings("unused") // MapStruct generates and calls the implementation.
public abstract class StripeSessionLineItemListConverter {

    @Autowired
    protected StripeProperties stripeProperties;

    public abstract List<SessionCreateParams.LineItem> toLineItems(List<CartItemSnapshot> shoppingCartItems);

    @Mapping(target = "priceData.unitAmount", source = "product.price", qualifiedByName = "toStripeUnitAmount")
    @Mapping(target = "priceData.currency", expression = "java(stripeProperties.currency())")
    @Mapping(target = "priceData.productData.name", source = "product.name")
    @Mapping(target = "quantity", source = "productQuantity")
    public abstract SessionCreateParams.LineItem toLineItem(CartItemSnapshot shoppingCartItem);

    @Named("toStripeUnitAmount")
    Long toStripeUnitAmount(final BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}

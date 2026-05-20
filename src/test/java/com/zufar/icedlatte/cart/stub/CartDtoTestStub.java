package com.zufar.icedlatte.cart.stub;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CartDtoTestStub {

    public static final UUID FIRST_PRODUCT_ID = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
    public static final UUID SECOND_PRODUCT_ID = UUID.fromString("2ade78e3-aa45-4b6b-adf4-86f8302ced7d");
    public static final UUID THIRD_PRODUCT_ID = UUID.fromString("b58ac6f1-7ee1-4888-9055-3bebb6aa3631");

    public static ShoppingCartItem createShoppingCartItem() {
        return ShoppingCartItem.builder()
                .id(UUID.randomUUID())
                .version(1)
                .shoppingCart(new ShoppingCart())
                .productId(FIRST_PRODUCT_ID)
                .productQuantity(5)
                .build();
    }

    public static ShoppingCart createShoppingCart() {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingCart shoppingCart = new ShoppingCart();

        UUID firstItemId = UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddc");
        UUID secondItemId = UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241");
        UUID thirdItemId = UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906");

        Set<ShoppingCartItem> items = new HashSet<>();
        items.add(ShoppingCartItem.builder().id(firstItemId).version(1).shoppingCart(shoppingCart).productId(FIRST_PRODUCT_ID).productQuantity(1).build());
        items.add(ShoppingCartItem.builder().id(secondItemId).version(1).shoppingCart(shoppingCart).productId(SECOND_PRODUCT_ID).productQuantity(2).build());
        items.add(ShoppingCartItem.builder().id(thirdItemId).version(1).shoppingCart(shoppingCart).productId(THIRD_PRODUCT_ID).productQuantity(3).build());

        shoppingCart.setId(UUID.randomUUID());
        shoppingCart.setUserId(userId);
        shoppingCart.setItems(items);
        shoppingCart.setItemsQuantity(3);
        shoppingCart.setProductsQuantity(6);
        shoppingCart.setCreatedAt(OffsetDateTime.now());
        shoppingCart.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingCart;
    }

    public static ShoppingCart createEmptyShoppingCart() {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setId(UUID.randomUUID());
        shoppingCart.setUserId(userId);
        shoppingCart.setItems(new HashSet<>());
        shoppingCart.setItemsQuantity(0);
        shoppingCart.setProductsQuantity(0);
        shoppingCart.setCreatedAt(OffsetDateTime.now());
        return shoppingCart;
    }

    public static Map<UUID, ProductInfoDto> createProductsById() {
        List<ProductInfoDto> products = List.of(
                createProductInfoDto(FIRST_PRODUCT_ID, "First test name", "First test description", BigDecimal.valueOf(1.1), 1),
                createProductInfoDto(SECOND_PRODUCT_ID, "Second test name", "Second test description", BigDecimal.valueOf(2.2), 2),
                createProductInfoDto(THIRD_PRODUCT_ID, "Third test name", "Third test description", BigDecimal.valueOf(3.3), 3)
        );
        return products.stream().collect(Collectors.toMap(ProductInfoDto::getId, Function.identity()));
    }

    public static ProductInfoDto createProductInfoDto(UUID id, String name, String description, BigDecimal price, int quantity) {
        ProductInfoDto dto = new ProductInfoDto();
        dto.setId(id);
        dto.setName(name);
        dto.setDescription(description);
        dto.setPrice(price);
        dto.setQuantity(quantity);
        dto.setActive(true);
        return dto;
    }

    public static ShoppingCartDto createShoppingCartDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        ShoppingCartDto dto = new ShoppingCartDto();
        dto.setUserId(userId);
        dto.setItemsQuantity(2);
        dto.setProductsQuantity(3);
        dto.setItemsTotalPrice(new BigDecimal("5.50"));
        dto.setCreatedAt(OffsetDateTime.now());
        dto.setClosedAt(OffsetDateTime.now().plusHours(2));
        return dto;
    }

}

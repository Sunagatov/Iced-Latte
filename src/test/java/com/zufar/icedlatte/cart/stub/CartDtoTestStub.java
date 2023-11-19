package com.zufar.icedlatte.cart.stub;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.product.entity.ProductInfo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CartDtoTestStub {

    public static ShoppingCartItem createShoppingCartItem() {
        ShoppingCart shoppingCart = new ShoppingCart();

        UUID productId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        ProductInfo productInfo = new ProductInfo(
                productId, "Test name", "Test description", BigDecimal.valueOf(1.1), 1, true);

        UUID itemId = UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddd");
        return new ShoppingCartItem(
                itemId, 1, shoppingCart, productInfo, 5);
    }

    public static NewShoppingCartItemDto createShoppingCartItemDtoToAdd() {
        NewShoppingCartItemDto newShoppingCartItemDto = new NewShoppingCartItemDto();
        newShoppingCartItemDto.setProductId(UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639"));
        newShoppingCartItemDto.setProductQuantity(3);

        return newShoppingCartItemDto;
    }

    public static ShoppingCartItemDto createShoppingCartItemDto() {
        ShoppingCartItemDto shoppingCartItemDto = new ShoppingCartItemDto();
        shoppingCartItemDto.setId(UUID.randomUUID());
        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639"));
        productInfoDto.setName("Test name");
        productInfoDto.setDescription("Test description");
        productInfoDto.setPrice(BigDecimal.valueOf(1.1));
        productInfoDto.setQuantity(3);
        productInfoDto.setActive(true);
        shoppingCartItemDto.setProductInfo(productInfoDto);

        return shoppingCartItemDto;
    }

    public static ShoppingCart createShoppingCart() {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingCart shoppingCart = new ShoppingCart();
        Set<ShoppingCartItem> items = new HashSet<>();

        UUID firstProductId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        ProductInfo firstProductInfo = new ProductInfo(
                firstProductId, "First test name", "First test description", BigDecimal.valueOf(1.1), 1, true);
        UUID secondProductId = UUID.fromString("2ade78e3-aa45-4b6b-adf4-86f8302ced7d");
        ProductInfo secondProductInfo = new ProductInfo(
                secondProductId, "Second test name", "Second test description", BigDecimal.valueOf(2.2), 2, true);
        UUID thirdProductId = UUID.fromString("b58ac6f1-7ee1-4888-9055-3bebb6aa3631");
        ProductInfo thridProductInfo = new ProductInfo(
                thirdProductId, "Third test name", "Third test description", BigDecimal.valueOf(3.3), 3, true);

        UUID firstItemId = UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddc");
        ShoppingCartItem firstItem = new ShoppingCartItem(
                firstItemId, 1, shoppingCart, firstProductInfo, 1);
        UUID secondItemId = UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241");
        ShoppingCartItem secondItem = new ShoppingCartItem(
                secondItemId, 1, shoppingCart, secondProductInfo, 2);
        UUID thirdItemId = UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906");
        ShoppingCartItem thirdItem = new ShoppingCartItem(
                thirdItemId, 1, shoppingCart, thridProductInfo, 3);

        items.add(firstItem);
        items.add(secondItem);
        items.add(thirdItem);

        shoppingCart.setUserId(userId);
        shoppingCart.setItems(items);
        shoppingCart.setItemsQuantity(3);
        shoppingCart.setProductsQuantity(6);
        shoppingCart.setCreatedAt(OffsetDateTime.now());
        shoppingCart.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingCart;
    }

    public static ShoppingCartDto createShoppingCartDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        ShoppingCartDto shoppingCartDto = new ShoppingCartDto();

        shoppingCartDto.setUserId(userId);
        shoppingCartDto.setItemsQuantity(2);
        shoppingCartDto.setProductsQuantity(3);
        shoppingCartDto.setItemsTotalPrice(BigDecimal.valueOf(5.5));
        shoppingCartDto.setCreatedAt(OffsetDateTime.now());
        shoppingCartDto.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingCartDto;
    }

    public static List<ShoppingCartItemDto> createShoppingCartDtoList() {
        List<ShoppingCartItemDto> items = new ArrayList<>();

        UUID firstProductDtoId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        ProductInfoDto firstProductInfoDto = new ProductInfoDto();
        firstProductInfoDto.setId(firstProductDtoId);
        firstProductInfoDto.setName("First test name");
        firstProductInfoDto.setDescription("First test description");
        firstProductInfoDto.setPrice(BigDecimal.valueOf(1.1));
        firstProductInfoDto.setQuantity(1);
        firstProductInfoDto.setActive(true);
        UUID secondProductId = UUID.fromString("2ade78e3-aa45-4b6b-adf4-86f8302ced7d");
        ProductInfoDto secondProductInfoDto = new ProductInfoDto();
        secondProductInfoDto.setId(secondProductId);
        secondProductInfoDto.setName("Second test name");
        secondProductInfoDto.setDescription("Second test description");
        secondProductInfoDto.setPrice(BigDecimal.valueOf(2.2));
        secondProductInfoDto.setQuantity(2);
        secondProductInfoDto.setActive(true);

        UUID firstItemId = UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddc");
        ShoppingCartItemDto firstItemDto = new ShoppingCartItemDto();
        firstItemDto.setId(firstItemId);
        firstItemDto.setProductInfo(firstProductInfoDto);
        firstItemDto.setProductQuantity(1);
        UUID secondItemId = UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241");
        ShoppingCartItemDto secondItemDto = new ShoppingCartItemDto();
        secondItemDto.setId(secondItemId);
        secondItemDto.setProductInfo(secondProductInfoDto);
        secondItemDto.setProductQuantity(2);

        items.add(firstItemDto);
        items.add(secondItemDto);

        return items;
    }

    public static ShoppingCartDto createFullShoppingCartDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        ShoppingCartDto shoppingCartDto = new ShoppingCartDto();

        shoppingCartDto.setId(UUID.randomUUID());
        shoppingCartDto.setUserId(userId);
        shoppingCartDto.setItemsQuantity(3);
        shoppingCartDto.setProductsQuantity(6);
        shoppingCartDto.setItemsTotalPrice(BigDecimal.valueOf(15.4));
        shoppingCartDto.setCreatedAt(OffsetDateTime.now());
        shoppingCartDto.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingCartDto;
    }

    public static List<ShoppingCartItemDto> createFullShoppingCartDtoList() {
        List<ShoppingCartItemDto> items = new ArrayList<>();

        UUID firstProductDtoId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        ProductInfoDto firstProductInfoDto = new ProductInfoDto();
        firstProductInfoDto.setId(firstProductDtoId);
        firstProductInfoDto.setName("First test name");
        firstProductInfoDto.setDescription("First test description");
        firstProductInfoDto.setPrice(BigDecimal.valueOf(1.1));
        firstProductInfoDto.setQuantity(1);
        firstProductInfoDto.setActive(true);

        UUID secondProductId = UUID.fromString("2ade78e3-aa45-4b6b-adf4-86f8302ced7d");
        ProductInfoDto secondProductInfoDto = new ProductInfoDto();
        secondProductInfoDto.setId(secondProductId);
        secondProductInfoDto.setName("Second test name");
        secondProductInfoDto.setDescription("Second test description");
        secondProductInfoDto.setPrice(BigDecimal.valueOf(2.2));
        secondProductInfoDto.setQuantity(2);
        secondProductInfoDto.setActive(true);

        UUID thirdProductId = UUID.fromString("b58ac6f1-7ee1-4888-9055-3bebb6aa3631");
        ProductInfoDto thirdProductInfoDto = new ProductInfoDto();
        thirdProductInfoDto.setId(thirdProductId);
        thirdProductInfoDto.setName("Third test name");
        thirdProductInfoDto.setDescription("Third test description");
        thirdProductInfoDto.setPrice(BigDecimal.valueOf(3.3));
        thirdProductInfoDto.setQuantity(3);
        thirdProductInfoDto.setActive(true);

        UUID firstItemId = UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddc");
        ShoppingCartItemDto firstItemDto = new ShoppingCartItemDto();
        firstItemDto.setId(firstItemId);
        firstItemDto.setProductInfo(firstProductInfoDto);
        firstItemDto.setProductQuantity(1);

        UUID secondItemId = UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241");
        ShoppingCartItemDto secondItemDto = new ShoppingCartItemDto();
        secondItemDto.setId(secondItemId);
        secondItemDto.setProductInfo(secondProductInfoDto);
        secondItemDto.setProductQuantity(2);

        UUID thirdItemId = UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906");
        ShoppingCartItemDto thirdItemDto = new ShoppingCartItemDto();
        thirdItemDto.setId(thirdItemId);
        thirdItemDto.setProductInfo(thirdProductInfoDto);
        thirdItemDto.setProductQuantity(3);

        items.add(firstItemDto);
        items.add(secondItemDto);
        items.add(thirdItemDto);

        return items;
    }

    public static ShoppingCartDto createEmptyShoppingCartDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        ShoppingCartDto shoppingCartDto = new ShoppingCartDto();
        List<ShoppingCartItemDto> items = new ArrayList<>();

        shoppingCartDto.setId(UUID.randomUUID());
        shoppingCartDto.setUserId(userId);
        shoppingCartDto.setItems(items);
        shoppingCartDto.setItemsQuantity(0);
        shoppingCartDto.setProductsQuantity(0);
        shoppingCartDto.setItemsTotalPrice(BigDecimal.valueOf(0.0));
        shoppingCartDto.setCreatedAt(OffsetDateTime.now());
        shoppingCartDto.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingCartDto;
    }
}

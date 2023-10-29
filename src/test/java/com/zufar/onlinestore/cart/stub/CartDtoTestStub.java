package com.zufar.onlinestore.cart.stub;

import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.openapi.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.openapi.dto.ProductInfoDto;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.product.entity.ProductInfo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CartDtoTestStub {

    public static ShoppingSessionItem createShoppingSessionItem() {
        ShoppingSession shoppingSession = new ShoppingSession();

        UUID productId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        ProductInfo productInfo = new ProductInfo(
                productId, "Test name", "Test description", BigDecimal.valueOf(1.1), 1, true);

        UUID itemId = UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddd");
        return new ShoppingSessionItem(
                itemId, 1, shoppingSession, productInfo, 5);
    }

    public static NewShoppingSessionItemDto createShoppingSessionItemDtoToAdd() {
        NewShoppingSessionItemDto newShoppingSessionItemDto = new NewShoppingSessionItemDto();
        newShoppingSessionItemDto.setProductId(UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639"));
        newShoppingSessionItemDto.setProductQuantity(3);

        return newShoppingSessionItemDto;
    }

    public static ShoppingSessionItemDto createShoppingSessionItemDto() {
        ShoppingSessionItemDto shoppingSessionItemDto = new ShoppingSessionItemDto();
        shoppingSessionItemDto.setId(UUID.randomUUID());
        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639"));
        productInfoDto.setName("Test name");
        productInfoDto.setDescription("Test description");
        productInfoDto.setPrice(BigDecimal.valueOf(1.1));
        productInfoDto.setQuantity(3);
        productInfoDto.setActive(true);
        shoppingSessionItemDto.setProductInfo(productInfoDto);

        return shoppingSessionItemDto;
    }

    public static ShoppingSession createShoppingSession() {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingSession shoppingSession = new ShoppingSession();
        Set<ShoppingSessionItem> items = new HashSet<>();

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
        ShoppingSessionItem firstItem = new ShoppingSessionItem(
                firstItemId, 1, shoppingSession, firstProductInfo, 1);
        UUID secondItemId = UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241");
        ShoppingSessionItem secondItem = new ShoppingSessionItem(
                secondItemId, 1, shoppingSession, secondProductInfo, 2);
        UUID thirdItemId = UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906");
        ShoppingSessionItem thirdItem = new ShoppingSessionItem(
                thirdItemId, 1, shoppingSession, thridProductInfo, 3);

        items.add(firstItem);
        items.add(secondItem);
        items.add(thirdItem);

        shoppingSession.setUserId(userId);
        shoppingSession.setItems(items);
        shoppingSession.setItemsQuantity(3);
        shoppingSession.setProductsQuantity(6);
        shoppingSession.setCreatedAt(OffsetDateTime.now());
        shoppingSession.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingSession;
    }

    public static ShoppingSessionDto createShoppingSessionDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        ShoppingSessionDto shoppingSessionDto = new ShoppingSessionDto();

        shoppingSessionDto.setUserId(userId);
        shoppingSessionDto.setItemsQuantity(2);
        shoppingSessionDto.setProductsQuantity(3);
        shoppingSessionDto.setItemsTotalPrice(BigDecimal.valueOf(5.5));
        shoppingSessionDto.setCreatedAt(OffsetDateTime.now());
        shoppingSessionDto.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingSessionDto;
    }

    public static List<ShoppingSessionItemDto> createShoppingSessionDtoList() {
        List<ShoppingSessionItemDto> items = new ArrayList<>();

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
        ShoppingSessionItemDto firstItemDto = new ShoppingSessionItemDto();
        firstItemDto.setId(firstItemId);
        firstItemDto.setProductInfo(firstProductInfoDto);
        firstItemDto.setProductQuantity(1);
        UUID secondItemId = UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241");
        ShoppingSessionItemDto secondItemDto = new ShoppingSessionItemDto();
        secondItemDto.setId(secondItemId);
        secondItemDto.setProductInfo(secondProductInfoDto);
        secondItemDto.setProductQuantity(2);

        items.add(firstItemDto);
        items.add(secondItemDto);

        return items;
    }

    public static ShoppingSessionDto createFullShoppingSessionDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        ShoppingSessionDto shoppingSessionDto = new ShoppingSessionDto();

        shoppingSessionDto.setId(UUID.randomUUID());
        shoppingSessionDto.setUserId(userId);
        shoppingSessionDto.setItemsQuantity(3);
        shoppingSessionDto.setProductsQuantity(6);
        shoppingSessionDto.setItemsTotalPrice(BigDecimal.valueOf(15.4));
        shoppingSessionDto.setCreatedAt(OffsetDateTime.now());
        shoppingSessionDto.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingSessionDto;
    }

    public static List<ShoppingSessionItemDto> createFullShoppingSessionDtoList() {
        List<ShoppingSessionItemDto> items = new ArrayList<>();

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
        ShoppingSessionItemDto firstItemDto = new ShoppingSessionItemDto();
        firstItemDto.setId(firstItemId);
        firstItemDto.setProductInfo(firstProductInfoDto);
        firstItemDto.setProductQuantity(1);

        UUID secondItemId = UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241");
        ShoppingSessionItemDto secondItemDto = new ShoppingSessionItemDto();
        secondItemDto.setId(secondItemId);
        secondItemDto.setProductInfo(secondProductInfoDto);
        secondItemDto.setProductQuantity(2);

        UUID thirdItemId = UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906");
        ShoppingSessionItemDto thirdItemDto = new ShoppingSessionItemDto();
        thirdItemDto.setId(thirdItemId);
        thirdItemDto.setProductInfo(thirdProductInfoDto);
        thirdItemDto.setProductQuantity(3);

        items.add(firstItemDto);
        items.add(secondItemDto);
        items.add(thirdItemDto);

        return items;
    }

    public static ShoppingSessionDto createEmptyShoppingSessionDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        ShoppingSessionDto shoppingSessionDto = new ShoppingSessionDto();
        List<ShoppingSessionItemDto> items = new ArrayList<>();

        shoppingSessionDto.setId(UUID.randomUUID());
        shoppingSessionDto.setUserId(userId);
        shoppingSessionDto.setItems(items);
        shoppingSessionDto.setItemsQuantity(0);
        shoppingSessionDto.setProductsQuantity(0);
        shoppingSessionDto.setItemsTotalPrice(BigDecimal.valueOf(0.0));
        shoppingSessionDto.setCreatedAt(OffsetDateTime.now());
        shoppingSessionDto.setClosedAt(OffsetDateTime.now().plusHours(2));

        return shoppingSessionDto;
    }
}

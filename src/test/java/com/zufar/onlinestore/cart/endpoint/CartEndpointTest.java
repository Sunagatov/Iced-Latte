package com.zufar.onlinestore.cart.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.openapi.dto.AddNewItemsToShoppingSessionRequest;
import com.zufar.onlinestore.openapi.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.openapi.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.openapi.dto.UpdateProductQuantityInShoppingSessionItemRequest;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.security.jwt.filter.JwtAuthenticationProvider;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.zufar.onlinestore.cart.endpoint.CartEndpoint.CART_URL;
import static com.zufar.onlinestore.product.util.ProductStub.generateProduct;
import static com.zufar.onlinestore.product.util.ProductStub.generateProductWithProductId;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartEndpoint.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class CartEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartApi cartApi;

    @MockBean
    private SecurityPrincipalProvider securityPrincipalProvider;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    void whenGetShoppingSessionByAuthenticatedUserThenReturnShoppingSession() throws Exception {
        UUID userId = UUID.randomUUID();
        Integer productQuantity = 3;

        ShoppingSessionItemDto shoppingSessionItem = new ShoppingSessionItemDto();
        shoppingSessionItem.setId(UUID.randomUUID());
        shoppingSessionItem.setProductInfo(generateProduct());
        shoppingSessionItem.setProductQuantity(productQuantity);

        ShoppingSessionDto shoppingSession = new ShoppingSessionDto();
        shoppingSession.setId(UUID.randomUUID());
        shoppingSession.setUserId(userId);
        shoppingSession.setItems(List.of(shoppingSessionItem));
        shoppingSession.setItemsQuantity(shoppingSession.getItems().size());
        shoppingSession.setProductsQuantity(productQuantity);

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(cartApi.getShoppingSessionByUserId(userId)).thenReturn(shoppingSession);

        mockMvc.perform(
                        get(CART_URL).contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].productInfo.name").value(shoppingSessionItem.getProductInfo().getName()))
                .andExpect(jsonPath("$.productsQuantity").value(shoppingSession.getProductsQuantity()));

    }

    @Test
    void whenAddNewItemToShoppingSessionThenReturnUpdatedShoppingSession() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Integer productQuantity = 3;

        ShoppingSessionItemDto shoppingSessionItem = new ShoppingSessionItemDto();
        shoppingSessionItem.setId(UUID.randomUUID());
        shoppingSessionItem.setProductInfo(generateProductWithProductId(productId));
        shoppingSessionItem.setProductQuantity(productQuantity);

        ShoppingSessionDto shoppingSession = new ShoppingSessionDto();
        shoppingSession.setId(UUID.randomUUID());
        shoppingSession.setUserId(userId);
        shoppingSession.setItems(List.of(shoppingSessionItem));
        shoppingSession.setItemsQuantity(shoppingSession.getItems().size());
        shoppingSession.setProductsQuantity(productQuantity);

        NewShoppingSessionItemDto newShoppingSessionItemDto = new NewShoppingSessionItemDto();
        newShoppingSessionItemDto.setProductId(productId);
        newShoppingSessionItemDto.setProductQuantity(productQuantity);

        AddNewItemsToShoppingSessionRequest shoppingSessionRequest = new AddNewItemsToShoppingSessionRequest();
        shoppingSessionRequest.setItems(Set.of(newShoppingSessionItemDto));

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(cartApi.addItemsToShoppingSession(shoppingSessionRequest.getItems())).thenReturn(shoppingSession);

        mockMvc.perform(
                        post(CART_URL + "/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(shoppingSessionRequest))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].productInfo.name").value(shoppingSessionItem.getProductInfo().getName()))
                .andExpect(jsonPath("$.productsQuantity").value(shoppingSession.getProductsQuantity()));
    }

    @Test
    void whenDeleteItemsFromShoppingSessionThenReturnEmptyShoppingSession() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID shoppingSessionItemId1 = UUID.randomUUID();
        UUID shoppingSessionItemId2 = UUID.randomUUID();

        ShoppingSessionDto emptyShoppingSession = new ShoppingSessionDto();
        emptyShoppingSession.setId(UUID.randomUUID());
        emptyShoppingSession.setUserId(userId);
        emptyShoppingSession.setItems(emptyList());
        emptyShoppingSession.setItemsQuantity(0);
        emptyShoppingSession.setProductsQuantity(0);

        DeleteItemsFromShoppingSessionRequest shoppingSessionRequest = new DeleteItemsFromShoppingSessionRequest();
        shoppingSessionRequest.setShoppingSessionItemIds(List.of(shoppingSessionItemId1, shoppingSessionItemId2));

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(cartApi.deleteItemsFromShoppingSession(shoppingSessionRequest)).thenReturn(emptyShoppingSession);

        mockMvc.perform(
                        delete(CART_URL + "/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(shoppingSessionRequest))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.productsQuantity").value(0));
    }

    @Test
    void whenUpdateProductQuantityInShoppingSessionItemThenReturnUpdatedShoppingSession() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID shoppingSessionItemId = UUID.randomUUID();
        Integer productsQuantity = 5;
        Integer productQuantityChange = 2;

        ShoppingSessionItemDto shoppingSessionItem = new ShoppingSessionItemDto();
        shoppingSessionItem.setId(shoppingSessionItemId);
        shoppingSessionItem.setProductInfo(generateProduct());
        shoppingSessionItem.setProductQuantity(productsQuantity);

        ShoppingSessionDto updatedShoppingSession = new ShoppingSessionDto();
        updatedShoppingSession.setId(UUID.randomUUID());
        updatedShoppingSession.setUserId(userId);
        updatedShoppingSession.setItems(List.of(shoppingSessionItem));
        updatedShoppingSession.setItemsQuantity(updatedShoppingSession.getItems().size());
        updatedShoppingSession.setProductsQuantity(productsQuantity + productQuantityChange);

        UpdateProductQuantityInShoppingSessionItemRequest shoppingSessionRequest = new UpdateProductQuantityInShoppingSessionItemRequest();
        shoppingSessionRequest.setShoppingSessionItemId(shoppingSessionItemId);
        shoppingSessionRequest.setProductQuantityChange(productQuantityChange);

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(cartApi.updateProductQuantityInShoppingSessionItem(shoppingSessionItemId, productQuantityChange))
                .thenReturn(updatedShoppingSession);

        mockMvc.perform(
                        patch(CART_URL + "/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(shoppingSessionRequest))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].productInfo.name").value(shoppingSessionItem.getProductInfo().getName()))
                .andExpect(jsonPath("$.productsQuantity").value(productsQuantity + productQuantityChange));
    }
}

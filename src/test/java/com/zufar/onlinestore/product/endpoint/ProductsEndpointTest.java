package com.zufar.onlinestore.product.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.openapi.dto.ProductInfoDto;
import com.zufar.onlinestore.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.onlinestore.product.exception.ProductNotFoundException;
import com.zufar.onlinestore.security.jwt.filter.JwtAuthenticationProvider;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static com.zufar.onlinestore.product.endpoint.ProductsEndpoint.PRODUCTS_URL;
import static com.zufar.onlinestore.product.util.ProductStub.buildSampleProducts;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductsEndpoint.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class ProductsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductApi productApi;

    @MockBean
    private JwtAuthenticationProvider provider;

    private ProductInfoDto productInfo;

    private ProductListWithPaginationInfoDto productList;

    @BeforeEach
    void setUp() {
        productInfo = Instancio.create(ProductInfoDto.class);
        productList = buildSampleProducts(0, 10, "name", Sort.Direction.ASC);
    }

    @Test
    void whenGetProductsSortedByNameDescThenReturnProducts() throws Exception {
        int page = 0;
        int size = 10;
        String sortAttribute = "name";
        Sort.Direction sortDirection = Sort.Direction.DESC;
        ProductListWithPaginationInfoDto products = buildSampleProducts(page, size, sortAttribute, sortDirection);

        when(productApi.getProducts(page, size, sortAttribute, sortDirection.name())).thenReturn(products);

        mockMvc.perform(get(PRODUCTS_URL)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort_attribute", sortAttribute)
                        .param("sort_direction", sortDirection.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.products[0].name").value("Product E"))
                .andExpect(jsonPath("$.totalElements").value(products.getTotalElements()));

        verify(productApi).getProducts(page, size, sortAttribute, sortDirection.name());
    }


    @Test
    void whenGetProductByIdThenReturn200() throws Exception {
        UUID productId = UUID.randomUUID();

        when(productApi.getProduct(productId)).thenReturn(productInfo);

        mockMvc.perform(get(PRODUCTS_URL + "/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productInfo.getId().toString()));

        verify(productApi).getProduct(productId);
    }

    @Test
    void whenGetProductsThenReturn200() throws Exception {
        int page = 1;
        int size = 10;
        String sortAttribute = "name";
        String defaultDirection = Sort.Direction.ASC.name();

        when(productApi.getProducts(page, size, sortAttribute, defaultDirection)).thenReturn(productList);

        mockMvc.perform(get(PRODUCTS_URL)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort_attribute", sortAttribute)
                        .param("sort_direction", defaultDirection)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.products.size()").value(productList.getProducts().size()));

        verify(productApi).getProducts(page, size, sortAttribute, defaultDirection);
    }

    @Test
    void whenNullProductIdThenReturn404() throws Exception {
        UUID productId = UUID.randomUUID();
        String errorDescription = buildErrorDescription(
                ProductsEndpoint.class.getName()
        );

        when(productApi.getProduct(productId)).thenThrow(new ProductNotFoundException(productId));

        MvcResult mvcResult = mockMvc.perform(get(PRODUCTS_URL + "/{productId}", productId))
                .andExpect(status().isNotFound())
                .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        ApiResponse<Void> expectedResponse = createExpectedErrorResponse(errorDescription, productId);
        String expectedResponseBody = objectMapper.writeValueAsString(expectedResponse);

        //assertThat(actualResponse).isEqualTo(expectedResponseBody); TODO fix the approach

        verify(productApi).getProduct(productId);
    }

    @Test
    void whenGetProductsSortedByNameAscThenReturnProducts() throws Exception {
        int page = 0;
        int size = 10;
        String sortAttribute = "name";
        Sort.Direction sortDirection = Sort.Direction.ASC;

        when(productApi.getProducts(page, size, sortAttribute, sortDirection.name())).thenReturn(productList);

        mockMvc.perform(get(PRODUCTS_URL)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort_attribute", sortAttribute)
                        .param("sort_direction", sortDirection.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.products[0].name").value("Product A"))
                .andExpect(jsonPath("$.totalElements").value(productList.getTotalElements()));

        verify(productApi).getProducts(page, size, sortAttribute, sortDirection.name());
    }

    private String buildErrorDescription(String className) {
        final int problematicCodeLine = 33;
        return String.format("Operation was failed in method: %s that belongs to the class: %s. Problematic code line: %d",
                "getProductById", className, problematicCodeLine);
    }

    private ApiResponse<Void> createExpectedErrorResponse(String errorDescription, UUID productId) {
        return new ApiResponse<>(
                null,
                Collections.singletonList(String.format("The product with productId = %s is not found.", productId)),
                errorDescription,
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now());
    }
}

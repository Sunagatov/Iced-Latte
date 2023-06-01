package com.zufar.onlinestore.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.onlinestore.customer.converter.CustomerDtoConverter;
import com.zufar.onlinestore.customer.dto.AddressDto;
import com.zufar.onlinestore.customer.dto.CustomerDto;
import com.zufar.onlinestore.customer.endpoint.CustomerEndpoint;
import com.zufar.onlinestore.customer.entity.Address;
import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.customer.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Profile("Aws-Profile") //TODO remove
@WebMvcTest(CustomerEndpoint.class)
class CustomerEndpointTest {

    @MockBean
    private CustomerRepository customerCrudRepository;

    @MockBean
    private CustomerDtoConverter customerDtoConverter;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CUSTOMER_ID = "CustomerId";
    private static final String FIRST_NAME = "FirstName";
    private static final String LAST_NAME = "LastName";
    private static final String EMAIL = "zufar.sunagatov@gmail.com";
    private static final String LINE = "Line";
    private static final String CITY = "City";
    private static final String COUNTRY = "Country";

    private static final Address ADDRESS = Address.builder()
            .line(LINE)
            .city(CITY)
            .country(COUNTRY)
            .build();

    private static final Customer CUSTOMER = Customer.builder()
            .customerId(CUSTOMER_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .email(EMAIL)
            .address(ADDRESS)
            .build();

    private static final AddressDto ADDRESS_DTO = AddressDto.builder()
            .line(LINE)
            .city(CITY)
            .country(COUNTRY)
            .build();

    private static final CustomerDto CUSTOMER_DTO = CustomerDto.builder()
            .customerId(CUSTOMER_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .email(EMAIL)
            .address(ADDRESS_DTO)
            .build();

    @Test
    @DisplayName("CustomerEndpoint returns HttpStatus 'Created' when CustomerEndpoint.saveCustomer was executed successfully")
    void returnsHttpStatusCreatedWhenSaveCustomerWasCalled() throws Exception {
        doNothing()
                .when(customerCrudRepository).save(CUSTOMER);

        when(customerDtoConverter.convertToEntity(CUSTOMER_DTO))
                .thenReturn(CUSTOMER);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/customers")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsBytes(CUSTOMER_DTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    @DisplayName("CustomerEndpoint returns Customer when CustomerEndpoint.getCustomerById was called")
    void returnsCustomerWhenGetCustomerByIdWasCalled() throws Exception {
        when(customerCrudRepository.findById(Long.parseLong(CUSTOMER_ID)))
                .thenReturn(Optional.of(CUSTOMER));

        when(customerDtoConverter.convertToDto(CUSTOMER))
                .thenReturn(CUSTOMER_DTO);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/{id}", CUSTOMER_ID)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.customerId").value(CUSTOMER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.address.line").value(LINE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.address.city").value(CITY))
                .andExpect(MockMvcResultMatchers.jsonPath("$.address.country").value(COUNTRY));
    }

    @Test
    @DisplayName("CustomerEndpoint returns HttpStatus 'NotFound' when CustomerEndpoint.getCustomerById was called and returned null")
    void returnsNotFoundWhenGetCustomerByIdReturnsNull() throws Exception {
        when(customerCrudRepository.findById(Long.parseLong(CUSTOMER_ID)))
                .thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/{id}", CUSTOMER_ID)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("CustomerEndpoint returns HttpStatus 'OK' when CustomerEndpoint.deleteCustomerById was called")
    void returnsHttpStatusOkWhenDeleteCustomerByIdWasCalled() throws Exception {
        doNothing()
                .when(customerCrudRepository).deleteById(Long.parseLong(CUSTOMER_ID));

        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/customers/{id}", CUSTOMER_ID)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("CustomerEndpoint returns HttpStatus 'OK' when CustomerEndpoint.updateCustomer was called")
    void returnsHttpStatusOkWhenUpdateCustomerWasCalled() throws Exception {
        when(customerDtoConverter.convertToEntity(CUSTOMER_DTO))
                .thenReturn(CUSTOMER);

        CUSTOMER.setCustomerId(CUSTOMER_ID);

        doNothing()
                .when(customerCrudRepository).save(CUSTOMER);

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/customers/{id}", CUSTOMER_ID)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsBytes(CUSTOMER_DTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
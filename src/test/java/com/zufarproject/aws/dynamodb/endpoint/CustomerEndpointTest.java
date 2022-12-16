package com.zufarproject.aws.dynamodb.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufarproject.aws.dynamodb.dto.AddressDto;
import com.zufarproject.aws.dynamodb.dto.CustomerDto;
import com.zufarproject.aws.dynamodb.model.Address;
import com.zufarproject.aws.dynamodb.model.Customer;
import com.zufarproject.aws.dynamodb.repository.CrudRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;


@WebMvcTest(CustomerEndpoint.class)
class CustomerEndpointTest {

    @MockBean
    private CrudRepository<Customer> customerCrudRepository;

    @MockBean
    private ModelMapper modelMapper;

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
        Mockito.doNothing()
                .when(customerCrudRepository).save(CUSTOMER);

        Mockito.when(modelMapper.map(CUSTOMER_DTO, Customer.class))
                .thenReturn(CUSTOMER);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/customer")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(CUSTOMER_DTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    @DisplayName("CustomerEndpoint returns Customer when CustomerEndpoint.getCustomerById was called")
    void returnsCustomerWhenGetCustomerByIdWasCalled() throws Exception {
        Mockito.when(customerCrudRepository.getById(CUSTOMER_ID))
                .thenReturn(Optional.of(CUSTOMER));

        Mockito.when(modelMapper.map(CUSTOMER, CustomerDto.class))
                .thenReturn(CUSTOMER_DTO);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/customer/{id}", CUSTOMER_ID)
                        .contentType("application/json"))
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
        Mockito.when(customerCrudRepository.getById(CUSTOMER_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/customer/{id}", CUSTOMER_ID)
                        .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("CustomerEndpoint returns HttpStatus 'OK' when CustomerEndpoint.deleteCustomerById was called")
    void returnsHttpStatusOkWhenDeleteCustomerByIdWasCalled() throws Exception {
        Mockito.doNothing()
                .when(customerCrudRepository).deleteById(CUSTOMER_ID);

        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/customer/{id}", CUSTOMER_ID)
                        .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("CustomerEndpoint returns HttpStatus 'OK' when CustomerEndpoint.updateCustomer was called")
    void returnsHttpStatusOkWhenUpdateCustomerWasCalled() throws Exception {
        Mockito.when(modelMapper.map(CUSTOMER_DTO, Customer.class))
                .thenReturn(CUSTOMER);

        Mockito.doNothing()
                .when(customerCrudRepository).update(CUSTOMER_ID, CUSTOMER);

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/customer/{id}", CUSTOMER_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(CUSTOMER_DTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
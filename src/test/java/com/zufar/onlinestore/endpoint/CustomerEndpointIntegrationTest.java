package com.zufar.onlinestore.endpoint;

import com.zufar.onlinestore.customer.dto.AddressDto;
import com.zufar.onlinestore.customer.dto.CustomerDto;
import com.zufar.onlinestore.customer.entity.Address;
import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.customer.repository.dynamodb.CrudRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerEndpointIntegrationTest {

    @MockBean
    private CrudRepository<Customer> customerCrudRepository;

    @Value(value="${local.server.port}")
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
    public void testAddEmployee() {
        String url = String.format("http://localhost:%s/api/customers", port);

        ResponseEntity<Void> responseEntity = this.restTemplate
                .postForEntity(url, CUSTOMER_DTO, Void.class);

        assertEquals(HttpStatusCode.CREATED, responseEntity.getStatusCode().value());
    }

    @Test
    @DisplayName("CustomerEndpoint returns Customer when CustomerEndpoint.getCustomerById was called")
    void returnsCustomerWhenGetCustomerByIdWasCalled() {
        Mockito.when(customerCrudRepository.getById(CUSTOMER_ID))
                .thenReturn(Optional.of(CUSTOMER));

        String url = String.format("http://localhost:%s/api/customers/%s", port, CUSTOMER_ID);

        ResponseEntity<CustomerDto> responseEntity = this.restTemplate
                .postForEntity(url, CUSTOMER_DTO, CustomerDto.class);

        assertEquals(HttpStatusCode.OK, responseEntity.getStatusCode().value());
    }
}
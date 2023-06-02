package com.zufar.onlinestore.customer.endpoint;

import com.zufar.onlinestore.customer.converter.CustomerDtoConverter;
import com.zufar.onlinestore.customer.dto.CustomerDto;
import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.customer.repository.CustomerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.zufar.onlinestore.customer.endpoint.UrlConstants.API_CUSTOMERS;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = API_CUSTOMERS)
public class CustomerEndpoint {
    private final CustomerRepository customerCrudRepository;
    private final CustomerDtoConverter customerDtoConverter;

    @PostMapping
    public ResponseEntity<Void> saveCustomer(@RequestBody @Valid final CustomerDto saveCustomerRequest) {
        log.info("Received saveCustomerRequest to create Customer - {}.", saveCustomerRequest);
        Customer customerEntity = customerDtoConverter.convertToEntity(saveCustomerRequest);
        customerCrudRepository.save(customerEntity);
        log.info("The Customer was created");
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        log.info("Received request to get the Customer with id - {}.", customerId);
        Optional<Customer> customer = customerCrudRepository.findById(Long.parseLong(customerId));
        if (customer.isEmpty()) {
            log.info("the Customer with id - {} is absent.", customerId);
            return ResponseEntity.notFound()
                    .build();
        }
        CustomerDto customerDto = customerDtoConverter.convertToDto(customer.get());
        log.info("the Customer with id - {} was retrieved - {}.", customerId, customerDto);
        return ResponseEntity.ok()
                .body(customerDto);
    }

    @GetMapping
    public ResponseEntity<Collection<CustomerDto>> getAllCustomers() {
        log.info("Received request to get all customers");
        List<Customer> customerEntities = customerCrudRepository.findAll();
        if (customerEntities.isEmpty()) {
            log.info("All customers are absent.");
            return ResponseEntity.notFound()
                    .build();
        }
        Collection<CustomerDto> customers = customerEntities.stream()
                .map(customerDtoConverter::convertToDto)
                .toList();

        log.info("All customers were retrieved - {}.", customers);
        return ResponseEntity.ok()
                .body(customers);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        log.info("Received request to delete the Customer with id - {}.", customerId);
        customerCrudRepository.deleteById(Long.parseLong(customerId));
        log.info("the Customer with id - {} was deleted.", customerId);
        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCustomer(@PathVariable("id") @NotBlank final String customerId,
                                               @RequestBody @Valid @NotNull final CustomerDto updateCustomerRequest) {
        log.info("Received updateCustomerRequest to update the Customer with id - {}, updateCustomerRequest - {}.", customerId, updateCustomerRequest);
        Customer customerEntity = customerDtoConverter.convertToEntity(updateCustomerRequest);
        customerEntity.setCustomerId(customerId);
        customerCrudRepository.save(customerEntity);
        log.info("the Customer with id - {} was updated.", customerId);
        return ResponseEntity.ok()
                .build();
    }
}

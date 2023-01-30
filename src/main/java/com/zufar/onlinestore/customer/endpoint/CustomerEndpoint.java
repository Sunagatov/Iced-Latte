package com.zufar.onlinestore.customer.endpoint;

import com.zufar.onlinestore.customer.converter.CustomerDtoConverter;
import com.zufar.onlinestore.customer.dto.CustomerDto;
import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.repository.CrudRepository;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/customers")
public class CustomerEndpoint {
    private final CrudRepository<Customer> customerCrudRepository;
    private final CustomerDtoConverter customerDtoConverter;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> saveCustomer(@RequestBody @Valid @NotNull(message = "Customer is mandatory") final CustomerDto request) {
        log.info("Received request to create Customer - {}.", request);
        Customer customerEntity = customerDtoConverter.convertToEntity(request);
        customerCrudRepository.save(customerEntity);
        log.info("The Customer was created");
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        log.info("Received request to get the Customer with id - {}.", customerId);
        Optional<Customer> customer = customerCrudRepository.getById(customerId);
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
    @ResponseBody
    public ResponseEntity<Collection<CustomerDto>> getAllCustomers() {
        log.info("Received request to get all customers");
        Optional<Collection<Customer>> customerEntities = customerCrudRepository.getAll();
        if (customerEntities.isEmpty()) {
            log.info("All customers are absent.");
            return ResponseEntity.notFound()
                    .build();
        }
        Collection<CustomerDto> customers = customerEntities.get().stream()
                .map(customerDtoConverter::convertToDto)
                .toList();

        log.info("All customers were retrieved - {}.", customers);
        return ResponseEntity.ok()
                .body(customers);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        log.info("Received request to delete the Customer with id - {}.", customerId);
        customerCrudRepository.deleteById(customerId);
        log.info("the Customer with id - {} was deleted.", customerId);
        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> updateCustomer(@PathVariable("id") @NotBlank final String customerId,
                                               @RequestBody @Valid @NotNull final CustomerDto request) {
        log.info("Received request to update the Customer with id - {}, request - {}.", customerId, request);
        Customer customerEntity = customerDtoConverter.convertToEntity(request);
        customerCrudRepository.update(customerId, customerEntity);
        log.info("the Customer with id - {} was updated.", customerId);
        return ResponseEntity.ok()
                .build();
    }
}

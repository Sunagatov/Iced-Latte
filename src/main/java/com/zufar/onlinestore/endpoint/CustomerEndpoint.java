package com.zufar.onlinestore.endpoint;

import com.zufar.onlinestore.dto.CustomerDto;
import com.zufar.onlinestore.model.Customer;
import com.zufar.onlinestore.repository.CrudRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/customers")
public class CustomerEndpoint {
    private final CrudRepository<Customer> customerCrudRepository;
    private final ModelMapper modelMapper;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> saveCustomer(@RequestBody @Valid @NotNull(message = "Customer is mandatory") final CustomerDto request) {
        log.info("Received request to create Customer - {}.", request);
        Customer customerEntity = modelMapper.map(request, Customer.class);
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
        CustomerDto customerDto = modelMapper.map(customer.get(), CustomerDto.class);
        log.info("the Customer with id - {} was retrieved - {}.", customerId, customerDto);
        return ResponseEntity.ok()
                .body(customerDto);
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
        Customer customerEntity = modelMapper.map(request, Customer.class);
        customerCrudRepository.update(customerId, customerEntity);
        log.info("the Customer with id - {} was updated.", customerId);
        return ResponseEntity.ok()
                .build();
    }
}

package com.zufarproject.aws.dynamodb.endpoint;

import com.zufarproject.aws.dynamodb.dto.CustomerDto;
import com.zufarproject.aws.dynamodb.model.Customer;
import com.zufarproject.aws.dynamodb.repository.CustomerCrudRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
public class CustomerEndpoint {
    private final CustomerCrudRepository customerCrudRepository;
    private final ModelMapper modelMapper;

    @PostMapping("/customer")
    @ResponseBody
    public ResponseEntity<Void> saveCustomer(@RequestBody @Valid @NotNull final CustomerDto customer) {
        Customer customerEntity = modelMapper.map(customer, Customer.class);
        customerCrudRepository.saveCustomer(customerEntity);
        return ResponseEntity.ok()
                .build();
    }

    @GetMapping("/customer/{id}")
    @ResponseBody
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        Customer customer = customerCrudRepository.getCustomerById(customerId);
        CustomerDto customerDto = modelMapper.map(customer, CustomerDto.class);
        return ResponseEntity.ok()
                .body(customerDto);
    }

    @DeleteMapping("/customer/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        customerCrudRepository.deleteCustomerById(customerId);
        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/customer/{id}")
    @ResponseBody
    public ResponseEntity<Void> updateCustomer(@PathVariable("id") @NotBlank final String customerId,
                                               @RequestBody @Valid @NotNull final CustomerDto customer) {
        Customer customerEntity = modelMapper.map(customer, Customer.class);
        customerCrudRepository.updateCustomer(customerId, customerEntity);
        return ResponseEntity.ok()
                .build();
    }
}

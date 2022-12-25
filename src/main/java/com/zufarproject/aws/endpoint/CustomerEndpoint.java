package com.zufarproject.aws.endpoint;

import com.zufarproject.aws.dto.CustomerDto;
import com.zufarproject.aws.model.Customer;
import com.zufarproject.aws.repository.CrudRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/customers")
public class CustomerEndpoint {
    private final CrudRepository<Customer> customerCrudRepository;
    private final ModelMapper modelMapper;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> saveCustomer(@RequestBody @Valid @NotNull(message = "Customer is mandatory") final CustomerDto customer) {
        Customer customerEntity = modelMapper.map(customer, Customer.class);
        customerCrudRepository.save(customerEntity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        Optional<Customer> customer = customerCrudRepository.getById(customerId);
        if (customer.isEmpty()) {
            return ResponseEntity.notFound()
                    .build();
        }
        CustomerDto customerDto = modelMapper.map(customer.get(), CustomerDto.class);
        return ResponseEntity.ok()
                .body(customerDto);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCustomerById(@PathVariable("id") @NotBlank final String customerId) {
        customerCrudRepository.deleteById(customerId);
        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> updateCustomer(@PathVariable("id") @NotBlank final String customerId,
                                               @RequestBody @Valid @NotNull final CustomerDto customer) {
        Customer customerEntity = modelMapper.map(customer, Customer.class);
        customerCrudRepository.update(customerId, customerEntity);
        return ResponseEntity.ok()
                .build();
    }
}

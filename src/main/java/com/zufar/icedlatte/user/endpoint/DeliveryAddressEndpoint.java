package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.DeliveryAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(UserEndpoint.API_CUSTOMERS + "/addresses")
public class DeliveryAddressEndpoint {

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final DeliveryAddressService deliveryAddressService;

    @GetMapping
    public ResponseEntity<List<DeliveryAddressDto>> getAll() {
        var userId = securityPrincipalProvider.getUserId();
        log.debug("delivery_address.list_requested: userId={}", userId);
        return ResponseEntity.ok(deliveryAddressService.getAll(userId));
    }

    @PostMapping
    public ResponseEntity<DeliveryAddressDto> create(@Valid @RequestBody DeliveryAddressRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        DeliveryAddressDto created = deliveryAddressService.create(userId, request);
        log.info("delivery_address.created: userId={}, addressId={}", userId, created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<DeliveryAddressDto> update(@PathVariable UUID addressId,
                                                     @Valid @RequestBody DeliveryAddressRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        DeliveryAddressDto updated = deliveryAddressService.update(userId, addressId, request);
        log.info("delivery_address.updated: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable UUID addressId) {
        var userId = securityPrincipalProvider.getUserId();
        deliveryAddressService.delete(userId, addressId);
        log.info("delivery_address.deleted: userId={}, addressId={}", userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<DeliveryAddressDto> setDefault(@PathVariable UUID addressId) {
        var userId = securityPrincipalProvider.getUserId();
        DeliveryAddressDto updated = deliveryAddressService.setDefault(userId, addressId);
        log.info("delivery_address.default_changed: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }
}

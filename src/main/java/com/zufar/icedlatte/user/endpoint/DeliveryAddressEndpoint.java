package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.*;
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
    private final DeliveryAddressProvider provider;
    private final DeliveryAddressCreator creator;
    private final DeliveryAddressUpdater updater;
    private final DeliveryAddressDeleter deleter;
    private final DeliveryAddressDefaultSetter defaultSetter;

    @GetMapping
    public ResponseEntity<List<DeliveryAddressDto>> getAll() {
        var userId = securityPrincipalProvider.getUserId();
        log.info("delivery_address.get_all: userId={}", userId);
        return ResponseEntity.ok(provider.getAll(userId));
    }

    @PostMapping
    public ResponseEntity<DeliveryAddressDto> create(@Valid @RequestBody DeliveryAddressRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("delivery_address.create: userId={}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creator.create(userId, request));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<DeliveryAddressDto> update(@PathVariable UUID addressId,
                                                     @Valid @RequestBody DeliveryAddressRequest request) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("delivery_address.update: userId={}, addressId={}", userId, addressId);
        return ResponseEntity.ok(updater.update(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable UUID addressId) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("delivery_address.delete: userId={}, addressId={}", userId, addressId);
        deleter.delete(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<DeliveryAddressDto> setDefault(@PathVariable UUID addressId) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("delivery_address.set_default: userId={}, addressId={}", userId, addressId);
        return ResponseEntity.ok(defaultSetter.setDefault(userId, addressId));
    }
}

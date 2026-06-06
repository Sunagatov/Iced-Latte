package com.zufar.icedlatte.user.endpoint;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.openapi.user.api.DeliveryAddressesApi;
import com.zufar.icedlatte.user.service.DeliveryAddressService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class DeliveryAddressEndpoint implements DeliveryAddressesApi {

    private static final String ADDRESSES_URL = "/api/v1/users/addresses";

    private final DeliveryAddressService deliveryAddressService;
    private final CurrentUserIdProvider currentUserIdProvider;

    @Override
    @GetMapping(ADDRESSES_URL)
    public ResponseEntity<List<DeliveryAddressDto>> getDeliveryAddresses() {
        var userId = currentUserId();
        log.debug("delivery_address.list_requested: userId={}", userId);
        return ResponseEntity.ok(deliveryAddressService.getAll(userId));
    }

    @Override
    @PostMapping(ADDRESSES_URL)
    public ResponseEntity<DeliveryAddressDto> addDeliveryAddress(@Valid @RequestBody DeliveryAddressRequest request) {
        var userId = currentUserId();
        DeliveryAddressDto created = deliveryAddressService.create(userId, request);
        log.info("delivery_address.created: userId={}, addressId={}", userId, created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @PutMapping(ADDRESSES_URL + "/{addressId}")
    public ResponseEntity<DeliveryAddressDto> updateDeliveryAddress(
            @PathVariable UUID addressId, @Valid @RequestBody DeliveryAddressRequest request) {
        var userId = currentUserId();
        DeliveryAddressDto updated = deliveryAddressService.update(userId, addressId, request);
        log.info("delivery_address.updated: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }

    @Override
    @DeleteMapping(ADDRESSES_URL + "/{addressId}")
    public ResponseEntity<Void> deleteDeliveryAddress(@PathVariable UUID addressId) {
        var userId = currentUserId();
        deliveryAddressService.delete(userId, addressId);
        log.info("delivery_address.deleted: userId={}, addressId={}", userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping(ADDRESSES_URL + "/{addressId}/default")
    public ResponseEntity<DeliveryAddressDto> setDefaultDeliveryAddress(@PathVariable UUID addressId) {
        var userId = currentUserId();
        DeliveryAddressDto updated = deliveryAddressService.setDefault(userId, addressId);
        log.info("delivery_address.default_changed: userId={}, addressId={}", userId, updated.getId());
        return ResponseEntity.ok(updated);
    }

    private UUID currentUserId() {
        return currentUserIdProvider.getUserId();
    }
}

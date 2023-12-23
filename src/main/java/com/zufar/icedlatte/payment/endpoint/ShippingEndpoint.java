package com.zufar.icedlatte.payment.endpoint;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.payment.api.ShippingApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(ShippingEndpoint.SHIPPING_URL)
public class ShippingEndpoint implements com.zufar.icedlatte.openapi.shipping.api.ShippingApi {

    public static final String SHIPPING_URL = "/api/v1/shipping";

    private final ShippingApi shippingApi;

    @Override
    @PostMapping
    public ResponseEntity<ShippingInfoDto> createShipping(@RequestBody ShippingInfoDto shippingInfoDto) {
        ShippingInfoDto infoDto = shippingApi.create(shippingInfoDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(infoDto);
    }

    @Override
    @DeleteMapping("/{shippingId}")
    public ResponseEntity<Void> deleteShippingById(@PathVariable Long shippingId) {
        shippingApi.deleteById(shippingId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ShippingInfoDto>> getAllShippingByUserId(@PathVariable UUID userId) {
        List<ShippingInfoDto> deliveries = shippingApi.getAllByUserId(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(deliveries);
    }

    @Override
    @GetMapping("/{shippingId}")
    public ResponseEntity<ShippingInfoDto> getShippingById(@PathVariable Long shippingId) {
        ShippingInfoDto shipping = shippingApi.getById(shippingId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(shipping);
    }
}

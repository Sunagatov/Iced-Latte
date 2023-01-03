package com.zufarproject.aws.endpoint;

import com.zufarproject.aws.dto.PurchaseProductRequest;
import com.zufarproject.aws.service.PurchaseTransactionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/store")
public class StoreEndpoint {
	private final PurchaseTransactionHandler purchaseTransactionHandler;

	@PostMapping(value = "/products")
	@ResponseBody
	public ResponseEntity<Void> purchaseProduct(@RequestBody @Valid @NotNull(message = "PurchaseRequest is mandatory") final PurchaseProductRequest purchaseProductRequest) {
		purchaseTransactionHandler.processRequest(purchaseProductRequest);
		return ResponseEntity.status(HttpStatus.CREATED)
				.build();
	}
}

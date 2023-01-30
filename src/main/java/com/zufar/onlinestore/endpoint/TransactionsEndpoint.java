package com.zufar.onlinestore.endpoint;

import com.zufar.onlinestore.dto.TransactionRequest;
import com.zufar.onlinestore.service.PurchaseTransactionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/api/store")
public class TransactionsEndpoint {
	private final PurchaseTransactionHandler purchaseTransactionHandler;

	@PostMapping(value = "/transactions")
	@ResponseBody
	public ResponseEntity<Void> makeTransaction(@RequestBody @Valid @NotNull(message = "TransactionRequest body is mandatory") final TransactionRequest transactionRequest) {
		purchaseTransactionHandler.processRequest(transactionRequest);
		return ResponseEntity.status(HttpStatus.CREATED)
				.build();
	}

	@GetMapping(value = "/transactions")
	@ResponseBody
	public ResponseEntity<Collection<String>> getAllTransactions() {
		Collection<String> allTransactions = purchaseTransactionHandler.getAllTransactions();
		return ResponseEntity.ok()
				.body(allTransactions);
	}
}

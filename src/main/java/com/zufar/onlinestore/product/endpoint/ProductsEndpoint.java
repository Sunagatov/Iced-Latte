package com.zufar.onlinestore.product.endpoint;

import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;

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
@RequestMapping(value = "/api/products")
public class ProductsEndpoint {
	private final ProductInfoRepository productInfoRepository;
	private final ProductInfoDtoConverter productInfoDtoConverter;

	@PostMapping
	@ResponseBody
	public ResponseEntity<Void> saveProduct(@RequestBody @Valid @NotNull(message = "Request body is mandatory") final ProductInfoDto request) {
		log.info("Received request to create ProductInfo - {}.", request);
		ProductInfo productInfo = productInfoDtoConverter.convertToEntity(request);
		productInfoRepository.save(productInfo);
		log.info("The ProductInfo was created");
		return ResponseEntity.status(HttpStatus.CREATED)
				.build();
	}

	@GetMapping("/{id}")
	@ResponseBody
	public ResponseEntity<ProductInfoDto> getProductInfoById(@PathVariable("id") @NotBlank final String id) {
		log.info("Received request to get the ProductInfo with id - {}.", id);
		Optional<ProductInfo> ProductInfo = productInfoRepository.findById(Integer.parseInt(id));
		if (ProductInfo.isEmpty()) {
			log.info("the ProductInfo with id - {} is absent.", id);
			return ResponseEntity.notFound()
					.build();
		}
		ProductInfoDto ProductInfoDto = productInfoDtoConverter.convertToDto(ProductInfo.get());
		log.info("the ProductInfo with id - {} was retrieved - {}.", id, ProductInfoDto);
		return ResponseEntity.ok()
				.body(ProductInfoDto);
	}

	@GetMapping
	@ResponseBody
	public ResponseEntity<Collection<ProductInfoDto>> getAllProducts() {
		log.info("Received request to get all ProductInfos");
		Collection<ProductInfo> productInfoCollection = productInfoRepository.findAll();
		if (productInfoCollection.isEmpty()) {
			log.info("All ProductInfos are absent.");
			return ResponseEntity.notFound()
					.build();
		}
		Collection<ProductInfoDto> ProductInfos = productInfoCollection.stream()
				.map(productInfoDtoConverter::convertToDto)
				.toList();

		log.info("All ProductInfos were retrieved - {}.", ProductInfos);
		return ResponseEntity.ok()
				.body(ProductInfos);
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> deleteById(@PathVariable("id") @NotBlank final String id) {
		log.info("Received request to delete the ProductInfo with id - {}.", id);
		productInfoRepository.deleteById(Integer.parseInt(id));
		log.info("the ProductInfo with id - {} was deleted.", id);
		return ResponseEntity.ok()
				.build();
	}

	@PutMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> updateProductInfo(@PathVariable("id") @NotBlank final String id,
	                                              @RequestBody @Valid @NotNull final ProductInfoDto request) {
		log.info("Received request to update the ProductInfo with id - {}, request - {}.", id, request);
		ProductInfo productInfo = productInfoDtoConverter.convertToEntity(request);
		productInfo.setId(Integer.parseInt(id));
		productInfoRepository.save(productInfo);
		log.info("the ProductInfo with id - {} was updated.", id);
		return ResponseEntity.ok()
				.build();
	}
}

package com.zufar.onlinestore.configuration.mongo;

import com.zufar.onlinestore.dto.PriceDto;
import com.zufar.onlinestore.model.ProductInfo;
import com.zufar.onlinestore.repository.ProductInfoRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.math.BigDecimal;

@EnableMongoRepositories(basePackageClasses = ProductInfoRepository.class)
@Configuration
public class MongoConfig {

	@Bean
	CommandLineRunner commandLineRunner(ProductInfoRepository repository) {
		return strings -> {
			repository.save(
					new ProductInfo(
							1,
							"Product2",
							new PriceDto(BigDecimal.valueOf(256, 7), "GB"),
							"Category1"));
			repository.save(
					new ProductInfo(
							1,
							"Product2",
							new PriceDto(BigDecimal.valueOf(312, 7), "GB"),
							"Category1"));
			repository.save(
					new ProductInfo(
							1,
							"Product3",
							new PriceDto(BigDecimal.valueOf(123, 7), "GB"),
							"Category2"));
			repository.save(
					new ProductInfo(
							1,
							"Product4",
							new PriceDto(BigDecimal.valueOf(223, 7), "GB"),
							"Category2"));
		};
	}
}

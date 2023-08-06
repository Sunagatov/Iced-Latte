package com.zufar.onlinestore;

import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation;
import com.zufar.onlinestore.reservation.config.ReservationTimeoutConfiguration;
import com.zufar.onlinestore.reservation.service.ReservationCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@AllArgsConstructor
@EnableConfigurationProperties(value = {StripeConfiguration.class, ReservationTimeoutConfiguration.class})
public class OnlineStoreApplication implements CommandLineRunner {

    private final ReservationCreator reservationCreator;
    private final ProductInfoRepository productInfoRepository;

    public static void main(String[] args) {
        SpringApplication.run(OnlineStoreApplication.class, args);
    }

    @Override
    public void run(final String... args) throws Exception {


        List<ProductReservation> products = new ArrayList<>();
        products.add(new ProductReservation(UUID.fromString("1e5b295f-8f50-4425-90e9-8b590a27b3a9"), 5));

        var response = reservationCreator.tryToCreateReservation(
                new CreateReservationRequest(UUID.fromString("55ee2ba2-f64b-48b7-a9fb-13624c29a92a"), products)
        );

        System.out.println(response.reservations());
        productInfoRepository.findAll().forEach(System.out::println);

    }
}
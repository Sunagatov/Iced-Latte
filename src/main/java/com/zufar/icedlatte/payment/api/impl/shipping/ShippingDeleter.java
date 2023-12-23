package com.zufar.icedlatte.payment.api.impl.shipping;

import com.zufar.icedlatte.payment.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class ShippingDeleter {

    private final ShippingRepository shippingRepository;

    @Transactional
    public void deleteShippingById(Long shippingId) {
        shippingRepository.findById(shippingId).ifPresent(s -> {
            shippingRepository.deleteById(s.getShippingId());
            log.info("The shipping record was deleted from the database with id = {} ", shippingId);
        });
    }
}

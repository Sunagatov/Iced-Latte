package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductFilterOptionsProvider {

    private final ProductInfoRepository productInfoRepository;

    @Cacheable(cacheNames = "sellers")
    @Transactional(readOnly = true)
    public List<String> getSellerNames() {
        return productInfoRepository.findDistinctSellerNames();
    }

    @Cacheable(cacheNames = "brands")
    @Transactional(readOnly = true)
    public List<String> getBrandNames() {
        return productInfoRepository.findDistinctBrandNames();
    }
}

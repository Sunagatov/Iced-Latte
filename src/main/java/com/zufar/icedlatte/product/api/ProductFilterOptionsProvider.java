package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductFilterOptionsProvider {

    private final ProductInfoRepository productInfoRepository;

    @Transactional(readOnly = true)
    public List<String> getSellerNames() {
        return productInfoRepository.findDistinctSellerNames();
    }

    @Transactional(readOnly = true)
    public List<String> getBrandNames() {
        return productInfoRepository.findDistinctBrandNames();
    }
}

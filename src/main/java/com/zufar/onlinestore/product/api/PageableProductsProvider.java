package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;
import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageableProductsProvider {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductListWithPaginationInfoDto getProducts(final Integer page,
                                                        final Integer size,
                                                        final String sortAttribute,
                                                        final String sortDirection) {
        log.info("The ProductListWithPaginationInfoDto is called");
        Pageable pageable = createPageableObject(page, size, sortAttribute, sortDirection);
        log.info("The pageable object has been created");

        Page<ProductInfoDto> productsWithPageInfo = productInfoRepository
                .findAll(pageable)
                .map(productInfoDtoConverter::toDto);
        log.info("All products are found in the database given");

        return productInfoDtoConverter.toProductPaginationDto(productsWithPageInfo);
    }

    private Pageable createPageableObject(final Integer page,
                                          final Integer size,
                                          final String sortAttribute,
                                          final String sortDirection) {
        Sort sort;
        if (sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name()))
            sort = Sort.by(sortAttribute).ascending();
        else
            sort = Sort.by(sortAttribute).descending();

        return PageRequest.of(page, size, sort);
    }
}
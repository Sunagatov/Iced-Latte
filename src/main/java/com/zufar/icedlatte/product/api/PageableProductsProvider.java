package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class PageableProductsProvider {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductListWithPaginationInfoDto getProducts(final Integer page,
                                                        final Integer size,
                                                        final String sortAttribute,
                                                        final String sortDirection) {
        Pageable pageable = createPageableObject(page, size, sortAttribute, sortDirection);

        List<UUID> uuids = new ArrayList<>();
        List<ProductInfoDto> productInfoDtos = productInfoRepository.findAll(pageable).stream()
                .peek(productInfo -> uuids.add(productInfo.getProductId()))
                .map(productInfoDtoConverter::toDto)
                .toList();
        productInfoDtos = productPictureLinkUpdater.updateProductsFileUrl(productInfoDtos, uuids);
        Page<ProductInfoDto> productsWithPageInfo = new PageImpl<>(productInfoDtos);

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
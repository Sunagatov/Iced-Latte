package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductsProvider {

    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final ProductUpdater productUpdater;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<ProductInfoDto> getProducts(final List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }

        var dtosById = productInfoRepository.findAllById(uuids).stream()
                .map(productInfoDtoConverter::toDto)
                .map(productUpdater::update)
                .collect(Collectors.toMap(ProductInfoDto::getId, Function.identity()));

        var missing = new ArrayList<UUID>();
        var ordered = new ArrayList<ProductInfoDto>(uuids.size());

        for (UUID id : uuids) {
            var dto = dtosById.get(id);
            if (dto != null) ordered.add(dto);
            else missing.add(id);
        }

        if (!missing.isEmpty()) {
            log.error("Products with ids = {} are not found.", missing.stream()
                    .map(UUID::toString).collect(Collectors.joining(", ")));
            throw new ProductNotFoundException(missing);
        }
        return ordered;
    }
}

package com.zufar.icedlatte.order.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.pagination.PageRequestFactory;
import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class OrderPageRequestFactory {

    private static final Set<String> ALLOWED_SORT_ATTRIBUTES =
            Set.of("id", "createdAt", "updatedAt", "status", "itemsTotalPrice");

    private final PaginationConfig paginationConfig;
    private final PaginationParametersValidator paginationParametersValidator;

    Pageable build(Integer page, Integer size, String sortBy, String sortDirection) {
        PaginationConfig.Orders defaults = paginationConfig.orders();
        List<String> errors = new ArrayList<>(
                paginationParametersValidator.validate(page, size, sortBy, sortDirection, ALLOWED_SORT_ATTRIBUTES));
        if (size != null && size > defaults.maxPageSize()) {
            errors.add(error("'%s' is the incorrect 'size' value. Maximum allowed 'size' value is '%s'."
                    .formatted(size, defaults.maxPageSize())));
        }
        if (!errors.isEmpty()) {
            throw new BadRequestException("Order pagination parameters are incorrect. Error messages are [ %s ]."
                    .formatted(String.join(" ", errors)));
        }

        return PageRequestFactory.of(
                page != null ? page : paginationConfig.defaultPageNumber(),
                size != null ? size : defaults.defaultPageSize(),
                sortBy != null ? sortBy : defaults.defaultSortAttribute(),
                sortDirection != null ? sortDirection : defaults.defaultSortDirection());
    }

    private static String error(String message) {
        return " Error: { %s }. ".formatted(message);
    }
}

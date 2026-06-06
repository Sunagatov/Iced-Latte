package com.zufar.icedlatte.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PageRequestFactory {

    public static Pageable of(final int page, final int size, final String sortAttribute, final String sortDirection) {
        String normalizedSortAttribute = sortAttribute.trim();
        Sort sort = Sort.by(normalizedSortAttribute);
        sort = Sort.Direction.fromString(sortDirection.trim()) == Sort.Direction.ASC
                ? sort.ascending()
                : sort.descending();
        if (!"id".equals(normalizedSortAttribute)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }
}

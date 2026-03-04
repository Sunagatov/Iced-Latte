package com.zufar.icedlatte.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Utils {

    private Utils() {
    }

    public static Pageable createPageableObject(final Integer page,
                                                final Integer size,
                                                final String sortAttribute,
                                                final String sortDirection) {
        Sort sort = Sort.by(sortAttribute);
        sort = Sort.Direction.fromString(sortDirection) == Sort.Direction.ASC
                ? sort.ascending()
                : sort.descending();
        if (!"id".equals(sortAttribute)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }
}

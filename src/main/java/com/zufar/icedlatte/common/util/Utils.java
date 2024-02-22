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
        Sort sort;
        if (sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name()))
            sort = Sort.by(sortAttribute).ascending();
        else
            sort = Sort.by(sortAttribute).descending();

        return PageRequest.of(page, size, sort);
    }
}

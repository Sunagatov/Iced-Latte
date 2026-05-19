package com.zufar.icedlatte.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pagination")
public record PaginationConfig(
        @DefaultValue("0") int defaultPageNumber,
        @DefaultValue Products products,
        @DefaultValue Reviews reviews,
        @DefaultValue Orders orders
) {
    public record Products(
            @DefaultValue("50") int defaultPageSize,
            @DefaultValue("name") String defaultSortAttribute,
            @DefaultValue("desc") String defaultSortDirection
    ) {}

    public record Reviews(
            @DefaultValue("10") int defaultPageSize,
            @DefaultValue("createdAt") String defaultSortAttribute,
            @DefaultValue("desc") String defaultSortDirection
    ) {}

    public record Orders(
            @DefaultValue("10") int defaultPageSize,
            @DefaultValue("50") int maxPageSize,
            @DefaultValue("createdAt") String defaultSortAttribute,
            @DefaultValue("desc") String defaultSortDirection
    ) {}
}

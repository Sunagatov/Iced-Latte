package com.zufar.icedlatte.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pagination")
@Getter
@Setter
public class PaginationConfig {

    private int defaultPageNumber = 0;
    
    private Products products = new Products();
    private Reviews reviews = new Reviews();
    
    @Getter
    @Setter
    public static class Products {
        private int defaultPageSize = 50;
        private String defaultSortAttribute = "name";
        private String defaultSortDirection = "desc";
    }
    
    @Getter
    @Setter
    public static class Reviews {
        private int defaultPageSize = 10;
        private String defaultSortAttribute = "createdAt";
        private String defaultSortDirection = "desc";
    }
}
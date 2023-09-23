package com.zufar.onlinestore.product.util;

import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductStub {

    public static ProductListWithPaginationInfoDto buildSampleProducts(Integer page, Integer size, String sortAttribute, Sort.Direction sortDirection) {
        List<ProductInfoDto> products = generateSampleProducts();
        List<ProductInfoDto> sortedProducts = sortProducts(products, sortAttribute, sortDirection);

        return new ProductListWithPaginationInfoDto(sortedProducts, page, size, (long) sortedProducts.size(), calculateTotalPages(products.size(), size));
    }

    private static List<ProductInfoDto> generateSampleProducts() {
        List<ProductInfoDto> products = new ArrayList<>();

        products.add(new ProductInfoDto(
                UUID.randomUUID(),
                "Product A",
                "Description for Product A",
                BigDecimal.valueOf(10.50),
                20
        ));

        products.add(new ProductInfoDto(
                UUID.randomUUID(),
                "Product B",
                "Description for Product B",
                BigDecimal.valueOf(15.75),
                50
        ));

        products.add(new ProductInfoDto(
                UUID.randomUUID(),
                "Product C",
                "Description for Product C",
                BigDecimal.valueOf(20.00),
                30
        ));

        products.add(new ProductInfoDto(
                UUID.randomUUID(),
                "Product D",
                "Description for Product D",
                BigDecimal.valueOf(5.25),
                40
        ));

        products.add(new ProductInfoDto(
                UUID.randomUUID(),
                "Product E",
                "Description for Product E",
                BigDecimal.valueOf(12.00),
                60
        ));
        return products;
    }

    private static List<ProductInfoDto> sortProducts(List<ProductInfoDto> products, String sortAttribute, Sort.Direction direction) {
        Map<String, Comparator<ProductInfoDto>> attributeToComparator = Map.of(
                "name", Comparator.comparing(ProductInfoDto::name),
                "description", Comparator.comparing(ProductInfoDto::description),
                "price", Comparator.comparing(ProductInfoDto::price),
                "quantity", Comparator.comparing(ProductInfoDto::quantity)
        );

        Comparator<ProductInfoDto> comparator = attributeToComparator.getOrDefault(sortAttribute.toLowerCase(), null);

        if (comparator == null) {
            throw new IllegalArgumentException("Unsupported sort attribute: " + sortAttribute);
        }

        if (direction == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }
        return products.stream().sorted(comparator).toList();
    }

    private static Integer calculateTotalPages(Integer totalElements, Integer size) {
        return (totalElements + size - 1) / size;
    }
}

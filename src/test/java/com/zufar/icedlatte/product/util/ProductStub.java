package com.zufar.icedlatte.product.util;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
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

        ProductListWithPaginationInfoDto productList = new ProductListWithPaginationInfoDto();
        productList.setProducts(sortedProducts);
        productList.setPage(page);
        productList.setSize(size);
        productList.setTotalElements((long) sortedProducts.size());
        productList.setTotalPages(calculateTotalPages(sortedProducts.size(), size));

        return productList;
    }

    public static ProductInfo generateSampleEntityProduct() {
        ProductInfo productA = new ProductInfo();
        productA.setProductId(UUID.randomUUID());
        productA.setName("Product A");
        productA.setDescription("Description for Product A");
        productA.setPrice(BigDecimal.valueOf(10.50));
        productA.setQuantity(20);
        return productA;
    }

    public static List<ProductInfoDto> generateSampleProducts() {
        List<ProductInfoDto> products = new ArrayList<>();

        ProductInfoDto productA = new ProductInfoDto();
        productA.setId(UUID.randomUUID());
        productA.setName("Product A");
        productA.setDescription("Description for Product A");
        productA.setPrice(BigDecimal.valueOf(10.50));
        productA.setQuantity(20);

        ProductInfoDto productB = new ProductInfoDto();
        productB.setId(UUID.randomUUID());
        productB.setName("Product B");
        productB.setDescription("Description for Product B");
        productB.setPrice(BigDecimal.valueOf(15.75));
        productB.setQuantity(50);

        ProductInfoDto productC = new ProductInfoDto();
        productC.setId(UUID.randomUUID());
        productC.setName("Product C");
        productC.setDescription("Description for Product C");
        productC.setPrice(BigDecimal.valueOf(20.00));
        productC.setQuantity(30);

        ProductInfoDto productD = new ProductInfoDto();
        productD.setId(UUID.randomUUID());
        productD.setName("Product D");
        productD.setDescription("Description for Product D");
        productD.setPrice(BigDecimal.valueOf(5.25));
        productD.setQuantity(40);
        ;

        ProductInfoDto productE = new ProductInfoDto();
        productE.setId(UUID.randomUUID());
        productE.setName("Product E");
        productE.setDescription("Description for Product E");
        productE.setPrice(BigDecimal.valueOf(12.00));
        productE.setQuantity(60);

        products.add(productA);
        products.add(productB);
        products.add(productC);
        products.add(productD);
        products.add(productE);

        return products;
    }

    private static List<ProductInfoDto> sortProducts(List<ProductInfoDto> products, String sortAttribute, Sort.Direction direction) {
        Map<String, Comparator<ProductInfoDto>> attributeToComparator = Map.of(
                "name", Comparator.comparing(ProductInfoDto::getName),
                "description", Comparator.comparing(ProductInfoDto::getDescription),
                "price", Comparator.comparing(ProductInfoDto::getPrice),
                "quantity", Comparator.comparing(ProductInfoDto::getQuantity)
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

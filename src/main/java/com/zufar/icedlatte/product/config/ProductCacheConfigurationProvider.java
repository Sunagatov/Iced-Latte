package com.zufar.icedlatte.product.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.config.CacheConfigurationProvider;
import com.zufar.icedlatte.common.config.CacheProperties;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

@Component
@RequiredArgsConstructor
public class ProductCacheConfigurationProvider implements CacheConfigurationProvider {

    public static final String PRODUCT_BY_ID = "productById";
    public static final String PRODUCT_IMAGE_URL = "productImageUrl";
    public static final String PRODUCT_IMAGE_URLS = "productImageUrls";
    public static final String BRANDS = "brands";
    public static final String SELLERS = "sellers";

    private final CacheProperties cacheProperties;

    @Override
    public Set<String> cacheNames() {
        return Set.of(PRODUCT_BY_ID, PRODUCT_IMAGE_URL, PRODUCT_IMAGE_URLS, BRANDS, SELLERS);
    }

    @Override
    public Map<String, RedisCacheConfiguration> redisCacheConfigurations(RedisCacheConfiguration baseConfiguration) {
        var mapper = new tools.jackson.databind.ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        JavaType listOfString = typeFactory.constructCollectionType(List.class, String.class);

        var productSerializer = new JacksonJsonRedisSerializer<>(mapper, ProductInfoDto.class);
        var stringSerializer = new JacksonJsonRedisSerializer<>(mapper, String.class);
        var listStringSerializer = new JacksonJsonRedisSerializer<>(mapper, listOfString);

        return Map.of(
                PRODUCT_BY_ID,
                baseConfiguration
                        .entryTtl(cacheProperties.getProductTtl())
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(productSerializer)),
                PRODUCT_IMAGE_URL,
                baseConfiguration
                        .entryTtl(cacheProperties.getImageUrlTtl())
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer)),
                PRODUCT_IMAGE_URLS,
                baseConfiguration
                        .entryTtl(cacheProperties.getImageUrlsTtl())
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(listStringSerializer)),
                BRANDS,
                baseConfiguration
                        .entryTtl(cacheProperties.getBrandsTtl())
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(listStringSerializer)),
                SELLERS,
                baseConfiguration
                        .entryTtl(cacheProperties.getSellersTtl())
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(listStringSerializer)));
    }
}

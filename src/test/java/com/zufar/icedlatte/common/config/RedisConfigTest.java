package com.zufar.icedlatte.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisConfig unit tests")
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private Cache cache;

    @Test
    @DisplayName("creates a plain Jackson object mapper")
    void createsObjectMapper() {
        RedisConfig config = new RedisConfig(new CacheProperties(), List.of());

        ObjectMapper objectMapper = config.objectMapper();

        assertThat(objectMapper).isNotNull();
    }

    @Test
    @DisplayName("creates a string-based RedisTemplate bound to the provided connection factory")
    void createsStringRedisTemplate() {
        RedisConfig config = new RedisConfig(new CacheProperties(), List.of());

        RedisTemplate<String, String> template = config.redisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("builds cache manager with versioned named cache configurations")
    void buildsVersionedCacheManager() {
        CacheProperties properties = new CacheProperties();
        properties.setDefaultTtl(Duration.ofSeconds(15));
        properties.setProductTtl(Duration.ofSeconds(30));
        properties.setImageUrlTtl(Duration.ofSeconds(45));
        properties.setImageUrlsTtl(Duration.ofSeconds(60));
        properties.setBrandsTtl(Duration.ofSeconds(75));
        properties.setSellersTtl(Duration.ofSeconds(90));

        RedisConfig config = new RedisConfig(
                properties,
                List.of(new TestCacheConfigurationProvider(
                        Set.of("productById", "productImageUrl", "productImageUrls", "brands", "sellers"))));
        ReflectionTestUtils.setField(config, "appVersion", "42");

        RedisCacheManager manager = config.cacheManager(connectionFactory);
        manager.afterPropertiesSet();

        assertThat(manager.getCacheConfigurations())
                .containsKeys("productById", "productImageUrl", "productImageUrls", "brands", "sellers");

        RedisCacheConfiguration productConfig = manager.getCacheConfigurations().get("productById");
        RedisCacheConfiguration sellersConfig = manager.getCacheConfigurations().get("sellers");
        assertThat(productConfig).isNotNull();
        assertThat(sellersConfig).isNotNull();

        assertThat(productConfig.usePrefix()).isTrue();
        assertThat(productConfig.getKeyPrefixFor("productById")).isEqualTo("v42:productById::");
        assertThat(sellersConfig.getKeyPrefixFor("sellers")).isEqualTo("v42:sellers::");
    }

    @Test
    @DisplayName("cache error handler swallows cache operation failures")
    void cacheErrorHandlerSwallowsFailures() {
        RedisConfig config = new RedisConfig(new CacheProperties(), List.of());
        CacheErrorHandler errorHandler = config.errorHandler();
        assertThat(errorHandler).isNotNull();

        assertThat(cache).isNotNull();
        org.mockito.Mockito.when(cache.getName()).thenReturn("products");

        org.assertj.core.api.Assertions.assertThatCode(
                        () -> errorHandler.handleCacheGetError(new RuntimeException("boom"), cache, "p1"))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(
                        () -> errorHandler.handleCachePutError(new RuntimeException("boom"), cache, "p1", "value"))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(
                        () -> errorHandler.handleCacheEvictError(new RuntimeException("boom"), cache, "p1"))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(
                        () -> errorHandler.handleCacheClearError(new RuntimeException("boom"), cache))
                .doesNotThrowAnyException();
    }

    private record TestCacheConfigurationProvider(Set<String> cacheNames) implements CacheConfigurationProvider {

        @Override
        public Map<String, RedisCacheConfiguration> redisCacheConfigurations(
                RedisCacheConfiguration baseConfiguration) {
            return cacheNames.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Function.identity(),
                            cacheName -> baseConfiguration.entryTtl(
                                    cacheName.isBlank() ? Duration.ZERO : Duration.ofSeconds(30))));
        }
    }
}

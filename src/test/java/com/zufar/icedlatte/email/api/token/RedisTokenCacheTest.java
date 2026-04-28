package com.zufar.icedlatte.email.api.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.email.exception.IncorrectTokenException;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisTokenCache unit tests")
class RedisTokenCacheTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("addToken stores a serialized request under the namespaced key with TTL")
    void addTokenStoresSerializedRequestUnderNamespacedKeyWithTtl() {
        RedisTokenCache cache = cache();
        UserRegistrationRequest request =
                new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.addToken("123456789", request, TokenPurpose.EMAIL_VERIFICATION);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("email:token:123456789"),
                valueCaptor.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(15)));
        assertThat(valueCaptor.getValue()).contains("\"purpose\":\"EMAIL_VERIFICATION\"");
        assertThat(valueCaptor.getValue()).contains("\"email\":\"alice@example.com\"");
    }

    @Test
    @DisplayName("getToken returns the cached request when purpose matches")
    void getTokenReturnsCachedRequestWhenPurposeMatches() throws Exception {
        RedisTokenCache cache = cache();
        UserRegistrationRequest request =
                new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:token:123456789"))
                .thenReturn("{\"request\":" + objectMapper.writeValueAsString(request) + ",\"purpose\":\"PASSWORD_RESET\"}");

        UserRegistrationRequest result = cache.getToken("123456789", TokenPurpose.PASSWORD_RESET);

        assertThat(result).usingRecursiveComparison().isEqualTo(request);
    }

    @Test
    @DisplayName("getToken rejects missing cache entries")
    void getTokenRejectsMissingCacheEntries() {
        RedisTokenCache cache = cache();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:token:123456789")).thenReturn(null);

        assertThatThrownBy(() -> cache.getToken("123456789", TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(IncorrectTokenException.class);
    }

    @Test
    @DisplayName("getToken rejects tokens with the wrong purpose")
    void getTokenRejectsTokensWithWrongPurpose() throws Exception {
        RedisTokenCache cache = cache();
        UserRegistrationRequest request =
                new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:token:123456789"))
                .thenReturn("{\"request\":" + objectMapper.writeValueAsString(request) + ",\"purpose\":\"EMAIL_VERIFICATION\"}");

        assertThatThrownBy(() -> cache.getToken("123456789", TokenPurpose.PASSWORD_RESET))
                .isInstanceOf(IncorrectTokenException.class);
    }

    @Test
    @DisplayName("removeToken deletes the namespaced Redis key")
    void removeTokenDeletesNamespacedRedisKey() {
        RedisTokenCache cache = cache();

        cache.removeToken("123456789");

        verify(redisTemplate).delete("email:token:123456789");
    }

    private RedisTokenCache cache() {
        RedisTokenCache cache = new RedisTokenCache(redisTemplate, objectMapper);
        ReflectionTestUtils.setField(cache, "expireTimeMinutes", 15);
        return cache;
    }
}

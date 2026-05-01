package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTokenCache unit tests")
class EmailTokenCacheTest {

    @Mock private ExpiringKeyValueStore temporaryStore;

    private EmailTokenCache cache;

    @BeforeEach
    void setUp() {
        cache = new EmailTokenCache(temporaryStore);
        ReflectionTestUtils.setField(cache, "expireTimeMinutes", 15);
    }

    @Test
    @DisplayName("addToken stores a namespaced entry with TTL")
    void addTokenStoresNamespacedEntryWithTtl() {
        UserRegistrationRequest request = new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");

        cache.addToken("123456789", request, TokenPurpose.EMAIL_VERIFICATION);

        verify(temporaryStore).put(eq("email:token:123456789"), any(), eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("removeToken deletes the namespaced key")
    void removeTokenDeletesNamespacedKey() {
        cache.removeToken("123456789");

        verify(temporaryStore).remove("email:token:123456789");
    }
}

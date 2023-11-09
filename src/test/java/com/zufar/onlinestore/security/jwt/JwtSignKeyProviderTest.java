package com.zufar.onlinestore.security.jwt;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtSignKeyProviderTest {

    @InjectMocks
    private JwtSignKeyProvider jwtSignKeyProvider;

    @Mock
    private SecretKey key;

    private String secretKey = "TestSecretKey";

    private static MockedStatic<Keys> mockedKeys;

    @BeforeAll
    static void setUpOnce() {
        mockedKeys = mockStatic(Keys.class);
    }

    @AfterAll
    static void tearDownOnce() {
        mockedKeys.close();
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtSignKeyProvider, "secretKey", secretKey);
    }

    @Test
    @DisplayName("Given a secret key string, When getting a key, Then it should return a valid key")
    void shouldReturnValidKeyFromSecretKeyString() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        when(Keys.hmacShaKeyFor(keyBytes)).thenReturn(key);

        Key resultKey = jwtSignKeyProvider.get();

        assertNotNull(resultKey);
        assertTrue(key.equals(resultKey));
    }
}
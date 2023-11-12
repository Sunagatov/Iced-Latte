package com.zufar.onlinestore.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtSignKeyProvider Tests")
class JwtSignKeyProviderTest {

    @InjectMocks
    private JwtSignKeyProvider jwtSignKeyProvider;

    private final String secretKeyBase64 = "your-base64-encoded-secret-key"; // Replace with your base64 encoded secret key

    @BeforeEach
    void setUp() {
        // Manually injecting the secret key value
        ReflectionTestUtils.setField(jwtSignKeyProvider, "secretKey", secretKeyBase64);
    }

    @Test
    @DisplayName("Should correctly decode and generate Key")
    void shouldCorrectlyDecodeAndGenerateKey() {
        try (MockedStatic<Decoders> decodersMockedStatic = mockStatic(Decoders.class);
             MockedStatic<Keys> keysMockedStatic = mockStatic(Keys.class)) {

            byte[] decodedKeyBytes = new byte[32]; // Length should be appropriate for the key
            Key expectedKey = mock(Key.class);

            // Mocking the static calls
            decodersMockedStatic.when(() -> Decoders.BASE64.decode(secretKeyBase64)).thenReturn(decodedKeyBytes);
            keysMockedStatic.when(() -> Keys.hmacShaKeyFor(decodedKeyBytes)).thenReturn(expectedKey);

            Key actualKey = jwtSignKeyProvider.get();

            assertNotNull(actualKey, "Generated Key should not be null");
            assertSame(expectedKey, actualKey, "Generated Key should match the expected Key");
        }
    }
}


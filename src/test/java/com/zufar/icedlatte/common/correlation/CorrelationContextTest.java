package com.zufar.icedlatte.common.correlation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CorrelationContext Tests")
class CorrelationContextTest {
    
    @Test
    @DisplayName("Should generate a non-null 16-character correlation ID")
    void shouldGenerateCorrelationId() {
        String correlationId = CorrelationContext.getOrGenerate();
        assertNotNull(correlationId);
        assertEquals(16, correlationId.length());
    }
    
    @Test
    @DisplayName("Should run callable with correlation ID set and clear it after")
    void shouldRunWithCorrelationId() throws Exception {
        String testId = "test123456789012";
        
        String result = CorrelationContext.runWithCorrelationId(testId, () -> {
            assertEquals(testId, CorrelationContext.get());
            return "success";
        });
        
        assertEquals("success", result);
        assertNull(CorrelationContext.get());
    }
    
    @Test
    @DisplayName("Should run runnable with correlation ID set and clear it after")
    void shouldRunRunnableWithCorrelationId() {
        String testId = "test123456789012";
        
        CorrelationContext.runWithCorrelationId(testId, () -> assertEquals(testId, CorrelationContext.get()));
        
        assertNull(CorrelationContext.get());
    }
}
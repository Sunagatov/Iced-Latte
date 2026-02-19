package com.zufar.icedlatte.common.correlation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationContextTest {
    
    @Test
    void shouldGenerateCorrelationId() {
        String correlationId = CorrelationContext.getOrGenerate();
        assertNotNull(correlationId);
        assertEquals(16, correlationId.length());
    }
    
    @Test
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
    void shouldRunRunnableWithCorrelationId() {
        String testId = "test123456789012";
        
        CorrelationContext.runWithCorrelationId(testId, () -> {
            assertEquals(testId, CorrelationContext.get());
        });
        
        assertNull(CorrelationContext.get());
    }
}
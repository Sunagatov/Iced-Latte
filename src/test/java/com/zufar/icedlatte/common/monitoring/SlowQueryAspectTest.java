package com.zufar.icedlatte.common.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlowQueryAspect unit tests")
class SlowQueryAspectTest {

    @Mock private ProceedingJoinPoint pjp;
    @InjectMocks private SlowQueryAspect aspect;

    @Test
    @DisplayName("track returns result from join point")
    void trackReturnsResult() throws Throwable {
        when(pjp.proceed()).thenReturn("result");
        // threshold high so slow-query branch is not taken — no signature needed
        ReflectionTestUtils.setField(aspect, "thresholdMs", 100_000L);
        Object result = aspect.track(pjp);
        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("track propagates exception from join point")
    void trackPropagatesException() throws Throwable {
        when(pjp.proceed()).thenThrow(new RuntimeException("db error"));
        ReflectionTestUtils.setField(aspect, "thresholdMs", 100_000L);
        assertThatThrownBy(() -> aspect.track(pjp))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    @DisplayName("track logs slow write query when method starts with save")
    void trackLogsSlowWriteQuery() throws Throwable {
        ReflectionTestUtils.setField(aspect, "thresholdMs", 0L);
        Signature signature = mock(Signature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("saveAll");
        when(signature.getDeclaringType()).thenReturn(Object.class);
        when(pjp.proceed()).thenReturn(null);

        aspect.track(pjp);

        verify(pjp).proceed();
    }

    @Test
    @DisplayName("track logs slow read query when method starts with find")
    void trackLogsSlowReadQuery() throws Throwable {
        ReflectionTestUtils.setField(aspect, "thresholdMs", 0L);
        Signature signature = mock(Signature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("findById");
        when(signature.getDeclaringType()).thenReturn(Object.class);
        when(pjp.proceed()).thenReturn("entity");

        Object result = aspect.track(pjp);

        assertThat(result).isEqualTo("entity");
    }
}

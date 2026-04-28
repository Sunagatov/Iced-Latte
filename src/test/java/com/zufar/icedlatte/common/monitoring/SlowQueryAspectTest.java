package com.zufar.icedlatte.common.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlowQueryAspect unit tests")
class SlowQueryAspectTest {

    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private Signature signature;

    @InjectMocks
    private SlowQueryAspect aspect;

    @Nested
    @DisplayName("track")
    class Track {

        @Test
        @DisplayName("returns the join point result when execution completes within the threshold")
        void returnsJoinPointResultWithinThreshold() throws Throwable {
            ReflectionTestUtils.setField(aspect, "thresholdMs", 100_000L);
            when(joinPoint.proceed()).thenReturn("result");

            Object result = aspect.track(joinPoint);

            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
            verifyNoMoreInteractions(joinPoint, signature);
        }

        @Test
        @DisplayName("propagates the join point exception unchanged")
        void propagatesJoinPointExceptionUnchanged() throws Throwable {
            ReflectionTestUtils.setField(aspect, "thresholdMs", 100_000L);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("db error"));

            assertThatThrownBy(() -> aspect.track(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db error");

            verify(joinPoint).proceed();
            verifyNoMoreInteractions(joinPoint, signature);
        }

        @Test
        @DisplayName("reads signature metadata when a slow write query is detected")
        void readsSignatureMetadataWhenSlowWriteQueryIsDetected() throws Throwable {
            ReflectionTestUtils.setField(aspect, "thresholdMs", 0L);
            when(joinPoint.proceed()).thenReturn(null);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveAll");
            when(signature.getDeclaringType()).thenReturn(Object.class);

            Object result = aspect.track(joinPoint);

            assertThat(result).isNull();
            verify(joinPoint).proceed();
            verify(joinPoint, times(2)).getSignature();
            verify(signature).getName();
            verify(signature).getDeclaringType();
            verifyNoMoreInteractions(joinPoint, signature);
        }

        @Test
        @DisplayName("treats non-write repository methods as slow reads")
        void treatsNonWriteRepositoryMethodsAsSlowReads() throws Throwable {
            ReflectionTestUtils.setField(aspect, "thresholdMs", 0L);
            when(joinPoint.proceed()).thenReturn("entity");
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("findById");
            when(signature.getDeclaringType()).thenReturn(Object.class);

            Object result = aspect.track(joinPoint);

            assertThat(result).isEqualTo("entity");
            verify(joinPoint).proceed();
            verify(joinPoint, times(2)).getSignature();
            verify(signature).getName();
            verify(signature).getDeclaringType();
            verifyNoMoreInteractions(joinPoint, signature);
        }
    }
}

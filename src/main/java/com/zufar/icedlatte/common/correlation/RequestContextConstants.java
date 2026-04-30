package com.zufar.icedlatte.common.correlation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestContextConstants {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String SESSION_ID_HEADER = "X-Session-ID";
    public static final String SESSION_ID_MDC_KEY = "sessionId";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String CLIENT_TRACE_ID_MDC_KEY = "clientTraceId";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String USER_ID_MDC_KEY = "userId";
}

package com.zufar.icedlatte.common.exception.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemDetailFactory {

    private static final String SAFE_5XX_MESSAGE = "An internal server error occurred.";

    private final ProblemTypeUriFactory problemTypeUriFactory;

    public ProblemDetail build(String typeSlug,
                               String title,
                               HttpStatus status,
                               String detail) {
        return build(typeSlug, title, status, detail, List.of());
    }

    public ProblemDetail build(String typeSlug,
                               String title,
                               HttpStatus status,
                               String detail,
                               List<FieldError> errors) {
        String safeDetail = status.is5xxServerError() ? SAFE_5XX_MESSAGE : detail;

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, safeDetail);
        pd.setType(problemTypeUriFactory.buildUri(typeSlug));
        pd.setTitle(title);
        pd.setInstance(resolveInstance());
        pd.setProperty("timestamp", Instant.now().toString());
        if (errors != null && !errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }
        return pd;
    }

    private static URI resolveInstance() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return URI.create(sra.getRequest().getRequestURI());
        }
        return null;
    }

    public record FieldError(String field, String message) {}
}

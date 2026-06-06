package com.zufar.icedlatte.common.exception.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;

@Component
public class ProblemTypeUriFactory {

    private final String typeBaseUrl;

    public ProblemTypeUriFactory(@Value("${problem.type-base-url}") String typeBaseUrl) {
        String errorMessage = "problem.type-base-url must be configured";
        String normalizedBaseUrl = Objects.requireNonNull(typeBaseUrl, errorMessage).trim();
        if (normalizedBaseUrl.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        this.typeBaseUrl = normalizedBaseUrl.endsWith("/") ? normalizedBaseUrl : normalizedBaseUrl + "/";
    }

    public URI buildUri(String typeSlug) {
        return URI.create(build(typeSlug));
    }

    public String build(String typeSlug) {
        String normalizedTypeSlug = Objects.requireNonNull(typeSlug, "typeSlug must not be null").trim();
        if (normalizedTypeSlug.isBlank()) {
            throw new IllegalArgumentException("typeSlug must not be blank");
        }
        if (normalizedTypeSlug.contains(":")) {
            return normalizedTypeSlug;
        }
        return typeBaseUrl + normalizedTypeSlug;
    }
}

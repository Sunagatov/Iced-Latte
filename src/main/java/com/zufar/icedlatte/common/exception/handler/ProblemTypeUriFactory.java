package com.zufar.icedlatte.common.exception.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ProblemTypeUriFactory {

    private final String typeBaseUrl;

    public ProblemTypeUriFactory(@Value("${problem.type-base-url}") String typeBaseUrl) {
        if (typeBaseUrl == null || typeBaseUrl.isBlank()) {
            throw new IllegalArgumentException("problem.type-base-url must be configured");
        }
        this.typeBaseUrl = typeBaseUrl.endsWith("/") ? typeBaseUrl : typeBaseUrl + "/";
    }

    public URI buildUri(String typeSlug) {
        return URI.create(build(typeSlug));
    }

    public String build(String typeSlug) {
        return typeSlug.contains(":") ? typeSlug : typeBaseUrl + typeSlug;
    }
}

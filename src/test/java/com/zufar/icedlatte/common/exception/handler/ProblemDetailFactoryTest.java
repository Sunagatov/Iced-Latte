package com.zufar.icedlatte.common.exception.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailFactory unit tests")
class ProblemDetailFactoryTest {

    private final ProblemDetailFactory factory = new ProblemDetailFactory();

    @Test
    @DisplayName("build() sets type URI correctly")
    void setsTypeUri() {
        ProblemDetail pd = factory.build("validation-failed", "Validation failed", HttpStatus.BAD_REQUEST, "detail");
        assertThat(pd.getType()).isEqualTo(URI.create("https://iced-latte.uk/errors/validation-failed"));
    }

    @Test
    @DisplayName("build() with absolute URI slug uses it directly")
    void absoluteUriSlugUsedDirectly() {
        ProblemDetail pd = factory.build("about:blank", "Method Not Allowed", HttpStatus.METHOD_NOT_ALLOWED, "detail");
        assertThat(pd.getType()).isEqualTo(URI.create("about:blank"));
    }

    @Test
    @DisplayName("build() sets title, status, detail")
    void setsTitleStatusDetail() {
        ProblemDetail pd = factory.build("not-found", "Not Found", HttpStatus.NOT_FOUND, "Resource missing");
        assertThat(pd.getTitle()).isEqualTo("Not Found");
        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getDetail()).isEqualTo("Resource missing");
    }

    @Test
    @DisplayName("build() sets extension properties: timestamp, message, error")
    void setsExtensionProperties() {
        ProblemDetail pd = factory.build("test", "Title", HttpStatus.BAD_REQUEST, "some detail");
        Map<String, Object> props = Objects.requireNonNull(pd.getProperties());
        assertThat(props).containsKey("timestamp");
        assertThat(props.get("timestamp")).isNotNull();
        assertThat(props.get("message")).isEqualTo("some detail");
        assertThat(props.get("error")).isEqualTo("Title");
    }

    @Test
    @DisplayName("build() with errors sets the errors extension property")
    void setsErrorsExtension() {
        List<ProblemDetailFactory.FieldError> errors = List.of(
                new ProblemDetailFactory.FieldError("name", "must not be blank"));
        ProblemDetail pd = factory.build("validation-failed", "Validation failed", HttpStatus.BAD_REQUEST, "detail", errors);
        Map<String, Object> props = Objects.requireNonNull(pd.getProperties());
        assertThat(props.get("errors")).isEqualTo(errors);
    }

    @Test
    @DisplayName("5xx status sanitizes detail")
    void sanitizes5xxDetail() {
        ProblemDetail pd = factory.build("internal-error", "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR, "secret stack trace");
        assertThat(pd.getDetail()).isEqualTo("An internal server error occurred.");
        Map<String, Object> props = Objects.requireNonNull(pd.getProperties());
        assertThat(props.get("message")).isEqualTo("An internal server error occurred.");
    }
}

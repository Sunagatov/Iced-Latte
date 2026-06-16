package com.zufar.icedlatte.common.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RequestPathUtils unit tests")
class RequestPathUtilsTest {

    @Test
    @DisplayName("treats API root paths as application paths instead of public internet noise")
    void treatsApiRootPathsAsApplicationPaths() {
        assertThat(RequestPathUtils.isPublicInternetNoise("/api")).isFalse();
        assertThat(RequestPathUtils.isPublicInternetNoise("/api/v1/products")).isFalse();
    }

    @Test
    @DisplayName("treats actuator and docs root paths as application paths instead of public internet noise")
    void treatsActuatorAndDocsRootPathsAsApplicationPaths() {
        assertThat(RequestPathUtils.isPublicInternetNoise("/actuator")).isFalse();
        assertThat(RequestPathUtils.isPublicInternetNoise("/actuator/health")).isFalse();
        assertThat(RequestPathUtils.isPublicInternetNoise("/api/docs")).isFalse();
        assertThat(RequestPathUtils.isPublicInternetNoise("/api/docs/swagger-ui/index.html")).isFalse();
    }

    @Test
    @DisplayName("treats unrelated paths as public internet noise")
    void treatsUnrelatedPathsAsPublicInternetNoise() {
        assertThat(RequestPathUtils.isPublicInternetNoise("/wp-login.php")).isTrue();
    }
}

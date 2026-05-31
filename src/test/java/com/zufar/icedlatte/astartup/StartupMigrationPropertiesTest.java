package com.zufar.icedlatte.astartup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("Startup migration properties tests")
class StartupMigrationPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);

    @Test
    @DisplayName("binds existing migration property names")
    void bindsExistingMigrationPropertyNames() {
        contextRunner
                .withPropertyValues(
                        "migration.timeout-minutes=7",
                        "migration.upload.enabled=true",
                        "migration.ratings.enabled=true")
                .run(context -> {
                    MigrationProperties properties = context.getBean(MigrationProperties.class);

                    assertThat(properties.timeout()).isEqualTo(Duration.ofMinutes(7));
                    assertThat(properties.upload().enabled()).isTrue();
                    assertThat(properties.ratings().enabled()).isTrue();
                });
    }

    @Test
    @DisplayName("binds duration timeout when supplied")
    void bindsDurationTimeoutWhenSupplied() {
        contextRunner
                .withPropertyValues("migration.timeout=PT30S", "migration.timeout-minutes=7")
                .run(context -> assertThat(
                                context.getBean(MigrationProperties.class).timeout())
                        .isEqualTo(Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("defaults migration toggles and timeout")
    void defaultsMigrationTogglesAndTimeout() {
        contextRunner.run(context -> {
            MigrationProperties properties = context.getBean(MigrationProperties.class);

            assertThat(properties.timeout()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.upload().enabled()).isFalse();
            assertThat(properties.ratings().enabled()).isFalse();
        });
    }

    @Test
    @DisplayName("fails fast when migration timeout is not positive")
    void failsFastWhenMigrationTimeoutIsNotPositive() {
        contextRunner
                .withPropertyValues("migration.timeout=PT0S")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("binds product image migration properties")
    void bindsProductImageMigrationProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.aws.buckets.products=products-bucket",
                        "spring.aws.default-image-directory.products=/seed/products")
                .run(context -> {
                    ProductImageMigrationProperties properties = context.getBean(ProductImageMigrationProperties.class);

                    assertThat(properties.productBucket()).isEqualTo("products-bucket");
                    assertThat(properties.productDirectoryPath()).isEqualTo("/seed/products");
                });
    }

    @EnableConfigurationProperties({MigrationProperties.class, ProductImageMigrationProperties.class})
    static class PropertiesConfiguration {}
}

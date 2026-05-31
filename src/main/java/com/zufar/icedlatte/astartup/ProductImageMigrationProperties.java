package com.zufar.icedlatte.astartup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "spring.aws")
public record ProductImageMigrationProperties(
        @DefaultValue Buckets buckets, @DefaultValue DefaultImageDirectory defaultImageDirectory) {

    public ProductImageMigrationProperties {
        buckets = buckets == null ? new Buckets("") : buckets;
        defaultImageDirectory = defaultImageDirectory == null ? new DefaultImageDirectory("") : defaultImageDirectory;
    }

    public String productBucket() {
        return buckets.products();
    }

    public String productDirectoryPath() {
        return defaultImageDirectory.products();
    }

    public record Buckets(@DefaultValue("") String products) {}

    public record DefaultImageDirectory(@DefaultValue("") String products) {}
}

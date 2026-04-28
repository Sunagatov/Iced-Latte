# syntax=docker/dockerfile:1.7

# =============================================================================
# Build stage
# Compiles the application and produces the executable Spring Boot JAR.
# =============================================================================

# Build the fat JAR in a separate stage so the runtime image stays small.
FROM maven:3.9-eclipse-temurin-25-alpine AS build

ARG BUILD_PROFILE

WORKDIR /workspace

# Copy only build metadata first for better dependency-layer caching.
# Copy the POM first so dependency resolution can stay cached across source edits.
COPY pom.xml ./

# Prime Maven dependencies.
# Warm the Maven cache via BuildKit. This is best-effort only: if go-offline misses
# something, the real package step below still resolves what it needs.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    (mvn -B -ntp dependency:go-offline || \
     echo "Maven go-offline failed; continuing with package step.")

# Copy application sources after dependency warmup so source edits do not invalidate
# the dependency cache layer.
COPY src ./src

# Package the app.
# Build the application JAR with the same cached Maven repository mount.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp clean package -DskipTests ${BUILD_PROFILE:+-P${BUILD_PROFILE}}

# =============================================================================
# Extract stage
# Splits the Spring Boot JAR into layers so Docker can reuse dependency layers.
# =============================================================================

# Spring Boot can split the executable JAR into layers so Docker reuses unchanged
# dependency layers between builds.
FROM eclipse-temurin:25-jre-alpine AS extract

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

# =============================================================================
# Runtime stage
# Contains only the JRE plus the extracted application layers.
# =============================================================================

# Final runtime image: only the JRE plus the extracted application layers.
FROM eclipse-temurin:25-jre-alpine

ARG IMAGE_VERSION=dev

# OCI image metadata helps registries and scanners identify the image cleanly.
LABEL org.opencontainers.image.title="Iced-Latte Backend" \
      org.opencontainers.image.description="Iced-Latte backend service" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.vendor="Iced-Latte Team"

# Run as non-root by default.
RUN addgroup -S app && adduser -S -h /app -G app app

WORKDIR /app

# Runtime defaults
# Keep runtime defaults here so operators can still override them with env vars.
# `SPRING_PROFILES_ACTIVE=prod` matches the intended container profile.
# `JAVA_TOOL_OPTIONS` is the least intrusive way to pass container-aware JVM flags.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Application layers
# These paths come from Spring Boot 4's current layer extraction layout.
COPY --link --from=extract --chown=app:app /app/app/dependencies/ ./
COPY --link --from=extract --chown=app:app /app/app/spring-boot-loader/ ./
COPY --link --from=extract --chown=app:app /app/app/snapshot-dependencies/ ./
COPY --link --from=extract --chown=app:app /app/app/application/ ./

# Runtime identity
USER app

# Network contract
EXPOSE 8083

# Startup command
# JarLauncher starts the extracted layered application without needing the original jar.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

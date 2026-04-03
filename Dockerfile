# =============================================================================
# BUILD STAGE — Modern 2026 approach with BuildKit cache mounts
# =============================================================================
FROM maven:3.9-eclipse-temurin-25-alpine AS build

# Build arguments
# BUILD_PROFILE: optional Maven profile for build-time optimizations.
# Leave empty by default because the project currently has no `prod` Maven profile.
ARG BUILD_PROFILE=

WORKDIR /app

# --- Copy POM first for dependency caching ---
COPY pom.xml ./

# --- Warm Maven cache ---
# Best-effort only: some ecosystems/plugins can break go-offline resolution even though
# the actual package build succeeds. Do not fail the image build at this stage.
RUN --mount=type=cache,target=/root/.m2 \
    (mvn -U dependency:go-offline -B --no-transfer-progress || \
     echo "⚠️ Maven go-offline failed; continuing with package step which will resolve dependencies directly.")

# --- Copy source code ---
COPY src ./src

# --- Build Application with cached dependencies ---
RUN --mount=type=cache,target=/root/.m2 \
    mvn -U package ${BUILD_PROFILE:+-P${BUILD_PROFILE}} -DskipTests -B --no-transfer-progress

# =============================================================================
# EXTRACT STAGE — split fat JAR into layers for Docker cache efficiency
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# =============================================================================
# CDS TRAINING STAGE — generate class-data sharing archive
# =============================================================================
# CDS archive creation is REQUIRED. If it fails, the build fails.
# Uses dedicated 'cds' profile with minimal safe defaults (no external services).
FROM eclipse-temurin:25-jre-alpine AS cds-train
WORKDIR /app
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
RUN java -XX:ArchiveClassesAtExit=app-cds.jsa \
        -Dspring.context.exit=onRefresh \
        -Dspring.profiles.active=cds \
        org.springframework.boot.loader.launch.JarLauncher && \
    if [ -f app-cds.jsa ] && [ -s app-cds.jsa ]; then \
        echo "CDS archive created successfully: $(du -h app-cds.jsa)"; \
    else \
        echo "ERROR: CDS archive creation failed or produced empty file"; \
        exit 1; \
    fi

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM eclipse-temurin:25-jre-alpine

# Build arguments for runtime
# IMAGE_VERSION: Docker image metadata version (does not change application version in JAR)
ARG IMAGE_VERSION=0.0.1

# OCI-compliant metadata labels
LABEL org.opencontainers.image.title="Iced-Latte Backend" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.description="Production-grade Java coffee marketplace backend" \
      org.opencontainers.image.vendor="Iced-Latte Team"

# --- Create non-root user for security hardening ---
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# --- Layered copy: dependencies change rarely, application layer changes every build ---
COPY --from=extract --chown=appuser:appgroup /app/dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appgroup /app/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/application/ ./
COPY --from=cds-train --chown=appuser:appgroup /app/app-cds.jsa ./

RUN mkdir -p /app/logs \
    && chown -R appuser:appgroup /app \
    && chmod 755 /app \
    && chmod 775 /app/logs

# --- Switch to non-root user ---
USER appuser

# --- Runtime Configuration ---
EXPOSE 8083

# --- Application Startup ---
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=60.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-XX:SharedArchiveFile=app-cds.jsa", \
    "-Dspring.profiles.active=prod", \
    "org.springframework.boot.loader.launch.JarLauncher"]

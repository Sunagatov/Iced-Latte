# =============================================================================
# BUILD STAGE — Modern 2026 approach with BuildKit cache mounts
# =============================================================================
FROM maven:3.9-eclipse-temurin-25-alpine AS build

# Build arguments
ARG MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ARG PROFILE=dev
ARG VERSION=0.0.1

WORKDIR /app

# --- Copy POM first for dependency caching ---
COPY pom.xml ./

# --- Download dependencies with BuildKit cache mount ---
# Cache persists on build machine, not in image layers
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B --no-transfer-progress

# --- Copy source code ---
COPY src ./src

# --- Build Application with cached dependencies ---
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -P${PROFILE} -DskipTests -B --no-transfer-progress

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
# CDS training is optional: if it fails, the build continues without CDS optimization.
# The runtime will fall back to normal class loading if app-cds.jsa is missing or invalid.
FROM eclipse-temurin:25-jre-alpine AS cds-train
WORKDIR /app
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
RUN java -XX:ArchiveClassesAtExit=app-cds.jsa \
        -Dspring.context.exit=onRefresh \
        -Dspring.profiles.active=prod \
        org.springframework.boot.loader.launch.JarLauncher 2>&1 || true && \
    if [ -f app-cds.jsa ]; then echo "CDS archive created: $(du -h app-cds.jsa)"; \
    else echo "CDS archive not created (non-critical, continuing)"; fi

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM eclipse-temurin:25-jre-alpine

# Build arguments for runtime
ARG VERSION=0.0.1

# Metadata
LABEL maintainer="Iced-Latte Team" \
      version="${VERSION}" \
      description="Iced-Latte Backend Application"

# --- Create non-root user for security hardening ---
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# --- Layered copy: dependencies change rarely, application layer changes every build ---
COPY --from=extract --chown=appuser:appgroup /app/dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appgroup /app/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/application/ ./
COPY --from=cds-train --chown=appuser:appgroup /app/app-cds.jsa ./ 2>/dev/null || true

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

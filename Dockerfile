# =============================================================================
# BUILD STAGE
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# --- Dependency Layer (cached when pom.xml unchanged) ---
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# --- Source Code Layer ---
COPY src ./src

# --- Build Application ---
RUN mvn versions:set-property -Dproperty=project.version -DnewVersion=0.0.1 && \
    mvn package -Pdev -DskipTests -B

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# --- Security Setup ---
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# --- Application Setup ---
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# --- Runtime Configuration ---
USER appuser
EXPOSE 8080

# --- Health Monitoring ---
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# --- Application Startup ---
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

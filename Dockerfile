# =============================================================================
# BUILD STAGE
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

# Build arguments
ARG MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ARG PROFILE=dev
ARG VERSION=0.0.1

WORKDIR /app

# --- Dependency Layer (cached when pom.xml unchanged) ---
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# --- Source Code Layer ---
COPY src ./src

# --- Build Application ---
RUN mvn versions:set-property -Dproperty=project.version -DnewVersion=${VERSION} && \
    mvn package -P${PROFILE} -DskipTests -B --no-transfer-progress

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM gcr.io/distroless/java21-debian12:nonroot

# Build arguments for runtime
ARG VERSION=0.0.1
ARG PROFILE=prod

# Metadata
LABEL maintainer="Iced-Latte Team" \
      version="${VERSION}" \
      description="Iced-Latte Backend Application"

WORKDIR /app

# --- Application Setup ---
COPY --from=build /app/target/*.jar app.jar

# --- Runtime Configuration ---
EXPOSE 8080

# --- Application Startup ---
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${PROFILE}", \
    "-jar", "app.jar"]

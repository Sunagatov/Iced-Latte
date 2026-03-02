# =============================================================================
# BUILD STAGE
# =============================================================================
FROM maven:3.9-eclipse-temurin-25-alpine AS build

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
# EXTRACT STAGE — split fat JAR into layers for Docker cache efficiency
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# =============================================================================
# CDS TRAINING STAGE — generate class-data sharing archive
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS cds-train
WORKDIR /app
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
RUN java -XX:ArchiveClassesAtExit=app-cds.jsa \
        -Dspring.context.exit=onRefresh \
        -Dspring.profiles.active=prod \
        org.springframework.boot.loader.launch.JarLauncher 2>/dev/null || true

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM eclipse-temurin:25-jre-alpine

# Build arguments for runtime
ARG VERSION=0.0.1
ARG PROFILE=prod

# Metadata
LABEL maintainer="Iced-Latte Team" \
      version="${VERSION}" \
      description="Iced-Latte Backend Application"

WORKDIR /app

# --- Layered copy: dependencies change rarely, application layer changes every build ---
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
COPY --from=cds-train /app/app-cds.jsa ./

# --- Runtime Configuration ---
EXPOSE 8080

# --- Application Startup ---
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=60.0", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+UseG1GC", \
    "-XX:G1HeapRegionSize=4m", \
    "-XX:+UseStringDeduplication", \
    "-XX:SharedArchiveFile=app-cds.jsa", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "org.springframework.boot.loader.launch.JarLauncher"]

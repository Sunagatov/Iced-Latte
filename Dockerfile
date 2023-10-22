#
# Build stage with Maven
#
FROM maven:3.8.3-openjdk-17 as maven_build

# Create builder user
RUN groupadd -r builder && useradd -r -g builder builder

WORKDIR /opt/app

# Ensure that the builder user has a maven repository directory
RUN mkdir -p /home/builder/.m2 && chown -R builder:builder /home/builder/.m2

# Copy only necessary files for dependency resolution
COPY pom.xml .env ./

# Set user to builder for safety
USER builder

# Resolve dependencies
RUN export APP_ENV=$(grep APP_ENV .env | cut -d '=' -f2) && \
    mvn dependency:go-offline -P${APP_ENV}

# Copy the source and build the application
COPY --chown=builder:builder . ./
RUN set -ex; \
    export APP_ENV=$(grep APP_ENV .env | cut -d '=' -f2) && \
    mvn versions:set-property -Dproperty=project.version -DnewVersion=${APP_VERSION} && \
    mvn package -P${APP_ENV}

#
# Production stage
#
FROM eclipse-temurin:17-jre-jammy as prod
WORKDIR /opt/app

# Install necessary dependencies and cleanup
RUN apt-get update && \
    apt-get install -y netcat acl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy compiled JAR, environment file, and entrypoint script
COPY --from=maven_build /opt/app/target/*.jar ./app.jar
COPY --from=maven_build /opt/app/.env ./.env
COPY --from=maven_build /opt/app/docker/docker-entrypoint.sh ./docker-entrypoint.sh

# Set permissions for entrypoint script
RUN chmod +x ./docker-entrypoint.sh

#
# Entrypoint
#
ENTRYPOINT ["./docker-entrypoint.sh"]

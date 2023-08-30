# Build stage with Maven
FROM maven:3.8.3-openjdk-17 as maven_build
ENV HOME=/opt/app
WORKDIR $HOME
COPY . ./
RUN export APP_ENV=$(grep APP_ENV .env | cut -d '=' -f2) && \
    export APP_VERSION=$(grep APP_VERSION .env | cut -d '=' -f2) && \
    mvn dependency:go-offline -P${APP_ENV} && \
    mvn versions:set-property -Dproperty=project.version -DnewVersion=${APP_VERSION} && \
    mvn package -P${APP_ENV}

# Package stage with JRE only
FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/app

# Install netcat
RUN apt-get update && apt-get install -y netcat

# Copy the JAR file and .env from the Maven build stage
COPY --from=maven_build /opt/app/target/*.jar /opt/app/app.jar
COPY --from=maven_build /opt/app/.env /opt/app/.env
COPY --from=maven_build /opt/app/docker/docker-entrypoint.sh /opt/app/docker-entrypoint.sh
RUN chmod +x /opt/app/docker-entrypoint.sh

# Set the entrypoint
ENTRYPOINT ["/opt/app/docker-entrypoint.sh"]

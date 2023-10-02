#
# Build stage with Maven
#
FROM maven:3.8.3-openjdk-17 as maven_build
ENV HOME=/opt/app
WORKDIR $HOME
COPY pom.xml ./
COPY .env ./
RUN set -ex; \
    export APP_ENV=$(grep APP_ENV .env | cut -d '=' -f2) && \
    mvn dependency:go-offline -DskipTests -P${APP_ENV}
COPY . ./
RUN chmod +x /opt/app/mvnw
RUN set -ex; \
    export APP_ENV=$(grep APP_ENV .env | cut -d '=' -f2) && \
    mvn versions:set-property -Dproperty=project.version -DnewVersion=${APP_VERSION} && \
    mvn package -P${APP_ENV}

#
# Production stage
#
FROM eclipse-temurin:17-jre-jammy as prod
WORKDIR /opt/app
RUN apt-get update && \
    apt-get install -y netcat && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
COPY --from=maven_build /opt/app/target/*.jar /opt/app/app.jar
COPY --from=maven_build /opt/app/.env /opt/app/.env
COPY --from=maven_build /opt/app/docker/docker-entrypoint.sh /opt/app/docker-entrypoint.sh
RUN chmod +x /opt/app/docker-entrypoint.sh

#
# Entrypoint
#
ENTRYPOINT ["/opt/app/docker-entrypoint.sh"]

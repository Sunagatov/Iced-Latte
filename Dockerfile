FROM maven:3.9-eclipse-temurin-17 as build
WORKDIR /usr/app
ADD . /usr/app
RUN set -ex; export APP_ENV=$(grep APP_ENV .env | cut -d '=' -f2) && \
    mvn versions:set-property -Dproperty=project.version -DnewVersion=${APP_VERSION} && \
    mvn package -P${APP_ENV}

FROM eclipse-temurin:17-jre-jammy 
WORKDIR /usr/app
RUN apt-get update && \
    apt-get install -y netcat acl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /usr/app/target/*.jar /usr/app/app.jar
COPY --from=build /usr/app/.env /usr/app/.env
COPY --from=build /usr/app/docker/docker-entrypoint.sh /usr/app/docker-entrypoint.sh

RUN chmod +x /usr/app/docker-entrypoint.sh

ENTRYPOINT ["/usr/app/docker-entrypoint.sh"]
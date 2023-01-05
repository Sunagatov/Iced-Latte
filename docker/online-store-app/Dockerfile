#Maven Build
FROM eclipse-temurin:17-jdk-jammy as builder
MAINTAINER Zufar Sunagatov <zufar.sunagatov@gmail.com>
USER root
RUN addgroup demogroup; adduser  --ingroup demogroup --disabled-password demo
USER demo
WORKDIR /opt/app

COPY ../../.mvn .mvn
COPY ../../mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY ../../src ./src
RUN ./mvnw clean install

#Run
FROM eclipse-temurin:17-jre-jammy
MAINTAINER Zufar Sunagatov <zufar.sunagatov@gmail.com>
USER root
WORKDIR /opt/app
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar" ]
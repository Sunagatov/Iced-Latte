#Maven Build
MAINTAINER zufar.sunagatov@gmail.com

FROM eclipse-temurin:17-jdk-jammy as builder
RUN addgroup demogroup; adduser  --ingroup demogroup --disabled-password demo
USER demo
WORKDIR /opt/app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY ./src ./src
RUN ./mvnw clean install

#Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/app
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar" ]
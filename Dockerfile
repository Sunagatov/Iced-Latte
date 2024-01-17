FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /usr/app
COPY . /usr/app
RUN mvn versions:set-property -Dproperty=project.version -DnewVersion=0.0.1 && \
    mvn package -Pdev -DskipTests

FROM eclipse-temurin:17-jre-jammy 
WORKDIR /usr/app
COPY --from=build /usr/app/target/*.jar /usr/app/app.jar
COPY --from=build /usr/app/.backend_env /usr/app/.backend_env
CMD ["java", "-jar", "/usr/app/app.jar"]
FROM maven:3.8.3-openjdk-17 as maven_build
ENV HOME=/opt/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD pom.xml $HOME
RUN mvn verify --fail-never
ADD ./ $HOME
RUN mvn package

FROM eclipse-temurin:17-jre-jammy
WORKDIR $HOME
COPY --from=maven_build /opt/app/target/online-store-0.0.1-SNAPSHOT.jar /opt/app/online-store-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/opt/app/online-store-0.0.1-SNAPSHOT.jar" ]

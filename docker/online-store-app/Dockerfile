
#
# MAVEN BUILD STAGE
#
FROM maven:3.8.3-openjdk-17 as maven_build
MAINTAINER Zufar Sunagatov <zufar.sunagatov@gmail.com>

ENV HOME=/opt/app
RUN mkdir -p $HOME
WORKDIR $HOME

ADD pom.xml $HOME
RUN mvn verify --fail-never
ADD . $HOME
RUN mvn package

#
# RUN APPLICATION ARTIFACT STAGE
#
FROM eclipse-temurin:17-jre-jammy
MAINTAINER Zufar Sunagatov <zufar.sunagatov@gmail.com>
WORKDIR $HOME
COPY --from=maven_build $HOME/target/*.jar $HOME/*.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "$HOME/*.jar" ]
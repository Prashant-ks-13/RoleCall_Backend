# Build context is the repository root (needed for the multi-module Maven reactor).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY rolecall-common/pom.xml rolecall-common/pom.xml
COPY application-service/pom.xml application-service/pom.xml
RUN mvn -q -pl application-service -am -DskipTests dependency:go-offline
COPY rolecall-common/src rolecall-common/src
COPY application-service/src application-service/src
RUN mvn -q -pl application-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/application-service/target/application-service.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]

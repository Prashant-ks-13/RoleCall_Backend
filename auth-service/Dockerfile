# Build context is the repository root (needed for the multi-module Maven reactor).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY rolecall-common/pom.xml rolecall-common/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
RUN mvn -q -pl auth-service -am -DskipTests dependency:go-offline
COPY rolecall-common/src rolecall-common/src
COPY auth-service/src auth-service/src
RUN mvn -q -pl auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

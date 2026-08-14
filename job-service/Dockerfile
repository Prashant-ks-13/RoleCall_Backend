# Build context is the repository root (needed for the multi-module Maven reactor).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY rolecall-common/pom.xml rolecall-common/pom.xml
COPY job-service/pom.xml job-service/pom.xml
RUN mvn -q -pl job-service -am -DskipTests dependency:go-offline
COPY rolecall-common/src rolecall-common/src
COPY job-service/src job-service/src
RUN mvn -q -pl job-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/job-service/target/job-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

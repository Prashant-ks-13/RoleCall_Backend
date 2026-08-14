# Build context is the repository root (needed for the multi-module Maven reactor).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY rolecall-common/pom.xml rolecall-common/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
RUN mvn -q -pl eureka-server -am -DskipTests dependency:go-offline
COPY rolecall-common/src rolecall-common/src
COPY eureka-server/src eureka-server/src
RUN mvn -q -pl eureka-server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eureka-server/target/eureka-server.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]

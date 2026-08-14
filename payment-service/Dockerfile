# Build context is the repository root (needed for the multi-module Maven reactor).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY rolecall-common/pom.xml rolecall-common/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
RUN mvn -q -pl payment-service -am -DskipTests dependency:go-offline
COPY rolecall-common/src rolecall-common/src
COPY payment-service/src payment-service/src
RUN mvn -q -pl payment-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

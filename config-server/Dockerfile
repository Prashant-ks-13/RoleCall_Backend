# Build context is the repository root (needed for the multi-module Maven reactor).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
RUN mvn -q -pl config-server -am -DskipTests dependency:go-offline
COPY config-server/src config-server/src
RUN mvn -q -pl config-server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server.jar app.jar
COPY config-server/config-repo ./config-repo
ENV CONFIG_REPO_PATH=/app/config-repo
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]

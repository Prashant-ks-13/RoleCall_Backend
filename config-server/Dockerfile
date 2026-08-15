# Build context is the repository root: Maven's multi-module reactor needs every
# module's pom.xml present to parse the build, even when -pl scopes the actual
# compile/package to just this module.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2/repository mvn -q -pl config-server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server.jar app.jar
COPY config-server/config-repo ./config-repo
ENV CONFIG_REPO_PATH=/app/config-repo
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]

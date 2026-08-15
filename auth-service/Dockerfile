# Build context is the repository root: Maven's multi-module reactor needs every
# module's pom.xml present to parse the build, even when -pl scopes the actual
# compile/package to just this module (and rolecall-common, its one dependency).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2/repository mvn -q -pl auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

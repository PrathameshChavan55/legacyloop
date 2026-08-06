# Built from the backend directory so the parent POM and the common module are in context:
#   docker build -f user-service/Dockerfile -t legacyloop-lite/user-service ./backend
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Every module's POM is copied because the parent lists all four in its reactor; only this
# service's sources are, so a change in another service does not invalidate this layer.
COPY pom.xml .
COPY common/pom.xml common/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY career-service/pom.xml career-service/
COPY social-service/pom.xml social-service/
# Resolve dependencies first so a code change does not re-download the world.
RUN mvn -q -pl common,user-service -am dependency:go-offline -B || true
COPY common/src common/src
COPY user-service/src user-service/src
RUN mvn -q -pl common,user-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

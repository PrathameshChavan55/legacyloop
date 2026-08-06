FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY common/pom.xml common/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY career-service/pom.xml career-service/
COPY social-service/pom.xml social-service/

RUN mvn -q -pl api-gateway -am dependency:go-offline -B || true

COPY api-gateway/src api-gateway/src
RUN mvn -q -pl api-gateway -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

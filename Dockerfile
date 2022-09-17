FROM maven:3.8.5-openjdk-18 AS build
COPY src /src
COPY pom.xml /
RUN --mount=type=cache,target=/root/.m2 mvn -f /pom.xml clean package -P dev -DskipTests=true

FROM openjdk:18-oracle
COPY --from=build target/*.jar /cloud-gateway.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=dev", "/cloud-gateway.jar"]
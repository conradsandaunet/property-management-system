# One Dockerfile for every Spring Boot module. Build from the repo root:
#   docker build --build-arg MODULE=resident-service .
# (security-common is a shared library that every module depends on, so the
# build context has to be the repo root.)

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build

COPY security-common/pom.xml security-common/pom.xml
COPY security-common/src security-common/src
RUN mvn -q -B -f security-common/pom.xml install -DskipTests

# Dependencies first, so source changes don't re-download them.
COPY ${MODULE}/pom.xml ${MODULE}/pom.xml
RUN mvn -q -B -f ${MODULE}/pom.xml dependency:go-offline

COPY ${MODULE}/src ${MODULE}/src
RUN mvn -q -B -f ${MODULE}/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]

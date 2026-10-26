# syntax=docker/dockerfile:1
# Image generique pour les microservices Spring Boot du backend.
# Usage : docker build -f devops-k8s/docker/spring-service.Dockerfile \
#           --build-arg SERVICE=customer-service --build-arg PORT=8082 \
#           -t fintech/customer-service:<sha> backend

# ---- Etape 1 : compilation (Maven + JDK 17) ----
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${SERVICE} -am package -DskipTests

# ---- Etape 2 : image finale (JRE seul) ----
FROM eclipse-temurin:17-jre-alpine
ARG SERVICE
ARG PORT=8080
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/${SERVICE}-*.jar app.jar
USER app
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
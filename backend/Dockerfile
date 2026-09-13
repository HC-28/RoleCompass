# ==============================================================================
# Backend Dockerfile — Spring Boot 3 / Java 17
# Two-stage build: Maven build → slim JRE runtime
# ==============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Cache dependency resolution separately from source compilation
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for the container health check
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy only the fat JAR from the build stage
COPY --from=build /build/target/*.jar app.jar

# Spring Boot port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

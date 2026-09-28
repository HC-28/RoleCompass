# ==============================================================================
# Backend Dockerfile — Spring Boot 3 / Java 17
# Two-stage build: Maven build → slim JRE runtime
# ==============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Cache dependency resolution separately from source compilation.
# pom.xml changes invalidate this layer; source changes do not.
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for the health check, then clean apt cache in the same layer
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as a non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser
USER appuser

# Copy only the fat JAR from the build stage
COPY --from=build --chown=appuser:appgroup /build/target/*.jar app.jar

# Spring Boot port
EXPOSE 8080

# Container-aware JVM: respect cgroup memory/CPU limits.
# -XX:+UseContainerSupport is ON by default in Java 11+ but made explicit here.
# -XX:MaxRAMPercentage caps heap at 75% of the container memory limit.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

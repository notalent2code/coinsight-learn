# syntax=docker/dockerfile:1
# Optimized Multi-stage Docker build for Maven + Amazon Corretto
# Usage: Replace config-server and 8888 with actual values

FROM maven:3.9-amazoncorretto-17 AS builder

WORKDIR /app

# Copy the parent POM first
COPY pom.xml .

# Create empty directories for all modules to satisfy Maven structure
RUN mkdir -p common gateway-service auth-service transaction-service ocr-service budget-service notification-service config-server

# Copy all module POM files
COPY common/pom.xml ./common/
COPY gateway-service/pom.xml ./gateway-service/
COPY auth-service/pom.xml ./auth-service/
COPY transaction-service/pom.xml ./transaction-service/
COPY ocr-service/pom.xml ./ocr-service/
COPY budget-service/pom.xml ./budget-service/
COPY notification-service/pom.xml ./notification-service/
COPY config-server/pom.xml ./config-server/

# Download dependencies with BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl config-server -am -B

# Copy only the source code we need for config-server
COPY common/src ./common/src
COPY config-server/src ./config-server/src

# Build config-server
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl config-server -am -DskipTests -B

# Runtime stage - minimal, secure image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy JAR file from builder stage with proper ownership
COPY --from=builder --chown=appuser:appgroup /app/config-server/target/config-server-*.jar app.jar

# Switch to non-root user
USER appuser

# Expose service port
EXPOSE 8888

# Health check for Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8888/actuator/health || exit 1

# Optimized JVM settings for containerized Spring Boot applications
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

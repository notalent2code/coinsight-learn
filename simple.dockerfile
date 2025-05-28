# Multi-stage build for all CoinSight microservices
FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /app
# Copy the entire project
COPY . .
# Build all services at once
RUN mvn clean package -DskipTests

# Runtime stage
FROM amazoncorretto:17-alpine
WORKDIR /app
ARG SERVICE_NAME
# Copy the specific service JAR
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

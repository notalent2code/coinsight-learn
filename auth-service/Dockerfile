FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /app
# Copy the entire project
COPY . /app/
# Build the specific module
RUN mvn clean package -pl auth-service -am -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]